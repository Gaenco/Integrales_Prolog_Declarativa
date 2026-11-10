:- encoding(utf8).
:- use_module(library(lists)).
:- use_module(library(apply)).
:- use_module(library(time)).

:- catch(set_prolog_flag(prefer_rationals, true), _, true).

%% --- Utilidades ---

contiene_x(T) :- T == x, !.
contiene_x(T) :-
    compound(T), T =.. [_|Args],
    member(A, Args), contiene_x(A), !.

es_const(T) :-
    \+ string(T), \+ is_list(T),
    \+ contiene_x(T),
    catch(V is T, _, fail),
    finito(V), !.

finito(V) :- float(V), !, V =:= V, abs(V) < 1.0e300.
finito(_).

entero_valor(V, M) :- integer(V), !, M = V.
entero_valor(V, M) :- float(V), V =:= truncate(V), !, M is truncate(V).

pot(1, x) :- !.
pot(M, x^M).

%% --- Entrada: texto a termino ---

parsear(Texto, T) :-
    normalizar_texto(Texto, N),
    (   catch(term_to_atom(T0, N), _, fail)
    ->  true
    ;   throw(msg('No pude interpretar la expresión. Revisa los paréntesis y usa * para multiplicar (ej: 3*x^2).'))
    ),
    (   ground(T0)
    ->  true
    ;   throw(msg('La expresión solo puede usar la variable x.'))
    ),
    normalizar_term(T0, T).

normalizar_texto(T, N) :-
    downcase_atom(T, L),
    atom_codes(L, C0),
    pot_codes(C0, C1),
    sep_ops(C1, C2),
    atom_codes(N, C2).

pot_codes([], []).
pot_codes([0'*, 0'*|T], [0'^|R]) :- !, pot_codes(T, R).
pot_codes([C|T], [C|R]) :- pot_codes(T, R).

sep_ops([], []).
sep_ops([C|T], [32, C, 32|R]) :- memberchk(C, `+-*/^`), !, sep_ops(T, R).
sep_ops([C|T], [C|R]) :- sep_ops(T, R).

alias(sh, sinh).  alias(ch, cosh).  alias(th, tanh).
alias(tg, tan).   alias(arctan, atan).  alias(arctg, atan).

normalizar_term(T, T) :- atomic(T), !.
normalizar_term(exp(A), e^B) :- !, normalizar_term(A, B).
normalizar_term(A/B, R) :- !,
    normalizar_term(A, A1), normalizar_term(B, B1),
    reescribir_div(A1, B1, R).
normalizar_term(T, T2) :-
    T =.. [F0|As],
    (   alias(F0, F) -> true ; F = F0 ),
    maplist(normalizar_term, As, Bs),
    T2 =.. [F|Bs].

reescribir_div(A, x, R) :- es_const(A), !,
    (   V is A, V =:= 1 -> R = x^(-1) ; R = A*x^(-1) ).
reescribir_div(A, x^N, R) :- es_const(A), es_const(N), !,
    (   V is A, V =:= 1 -> R = x^(-N) ; R = A*x^(-N) ).
reescribir_div(A, B, A/B).

parsear_limite(S, V) :-
    parsear(S, T),
    (   es_const(T)
    ->  V is T
    ;   throw(msg('Los límites deben ser números (ej: 0, 2.5, pi, 1/2).'))
    ).

%% --- Reglas de integracion ---

integral(K, [V-x]) :- es_const(K), !, V is K.

integral(x, [C-x^2]) :- !, C is 1/2.

integral(K*x, [C-x^2]) :- es_const(K), !, C is K/2.

integral(x^N, L) :-
    es_const(N), !,
    V is N,
    (   entero_valor(V, M) -> true
    ;   throw(msg('El exponente de x debe ser un número entero (x^n con n entero).'))
    ),
    (   M =:= -1
    ->  L = [1-log(abs(x))]
    ;   M1 is M + 1, C is 1/M1, pot(M1, B), L = [C-B]
    ).

integral(K*U, L) :- es_const(K), !,
    V is K, integral(U, LU), escalar(V, LU, L).
integral(U*K, L) :- es_const(K), !,
    V is K, integral(U, LU), escalar(V, LU, L).
integral(U/K, L) :- es_const(K), !,
    V is K,
    (   V =:= 0 -> throw(msg('División entre cero en la expresión.')) ; true ),
    W is 1/V, integral(U, LU), escalar(W, LU, L).
integral(-U, L) :- !,
    integral(U, LU), escalar(-1, LU, L).

integral(U+V, L) :- !,
    integral(U, LU), integral(V, LV), append(LU, LV, L).

integral(U-V, L) :- !,
    integral(U, LU), integral(V, LV),
    escalar(-1, LV, NLV), append(LU, NLV, L).

integral(A^x, L) :- es_const(A), !,
    V is A,
    (   V =< 0 -> throw(msg('La base de a^x debe ser un número positivo.')) ; true ),
    (   V =:= 1 -> L = [1-x]
    ;   A == e  -> L = [1-(e^x)]
    ;   ( number(A) -> Base = A ; Base = V ),
        L = [1-(Base^x/log(Base))]
    ).

integral(sin(x),  [-1-cos(x)])  :- !.
integral(cos(x),  [1-sin(x)])   :- !.
integral(sinh(x), [1-cosh(x)])  :- !.
integral(cosh(x), [1-sinh(x)])  :- !.

integral(tan(x), [-1-log(abs(cos(x)))]) :- !.

integral(atan(x), [1-(x*atan(x)), C-log(1+x^2)]) :- !, C is -1/2.

integral(tanh(x), [1-log(cosh(x))]) :- !.

integral(T, _) :- throw(no_soportado(T)).

escalar(_, [], []).
escalar(K, [C-B|R], [C2-B|R2]) :- C2 is K*C, escalar(K, R, R2).

%% --- Simplificacion ---

simplificar(L, S) :-
    foldl(insertar, L, [], F),
    exclude(coef_cero, F, S).

insertar(C-B, [], [C-B]).
insertar(C-B, [C0-B0|R], [C1-B0|R]) :- B0 == B, !, C1 is C0 + C.
insertar(T, [H|R], [H|R2]) :- insertar(T, R, R2).

coef_cero(C-_) :- C =:= 0.

%% --- Evaluacion y dominio ---

sust(x, X0, X0) :- !.
sust(T, X0, T2) :-
    compound(T), !,
    T =.. [F|As],
    maplist(sust_(X0), As, Bs),
    T2 =.. [F|Bs].
sust(T, _, T).
sust_(X0, A, B) :- sust(A, X0, B).

evaluar_F([], _, 0).
evaluar_F([C-B|R], X0, V) :-
    sust(B, X0, Be),
    Vb is C*Be,
    evaluar_F(R, X0, Vr),
    V is Vb + Vr.

pot_negativa(T) :- compound(T), T = (x^N), es_const(N), V is N, V < 0, !.
pot_negativa(T) :-
    compound(T), T =.. [_|As],
    member(A, As), pot_negativa(A), !.

contiene_func(T, F) :- compound(T), functor(T, F, 1), !.
contiene_func(T, F) :-
    compound(T), T =.. [_|As],
    member(A, As), contiene_func(A, F), !.

cruza_asintota_tan(Lo, Hi) :-
    K is ceiling((float(Lo) - pi/2)/pi - 1.0e-9),
    S is pi/2 + K*pi,
    S =< float(Hi) + 1.0e-12.

verificar_dominio(F, A, B) :-
    Lo is min(A, B), Hi is max(A, B),
    (   pot_negativa(F), Lo =< 0, 0 =< Hi
    ->  throw(msg('La función no está definida en x = 0 y el intervalo [a, b] lo contiene: la integral es impropia (diverge).'))
    ;   true ),
    (   contiene_func(F, tan), cruza_asintota_tan(Lo, Hi)
    ->  throw(msg('tan(x) no está definida en x = π/2 + kπ y el intervalo [a, b] contiene uno de esos puntos: la integral es impropia (diverge).'))
    ;   true ).

area_geometrica(F, A, B, Area) :-
    Lo is float(min(A, B)), Hi is float(max(A, B)),
    (   Lo =:= Hi -> Area = 0.0
    ;   N = 2000, H is (Hi - Lo)/N,
        simpson(F, Lo, H, N, 0, 0.0, S),
        Area is H/3*S ).

simpson(_, _, _, N, I, Acc, Acc) :- I > N, !.
simpson(F, Lo, H, N, I, Acc, S) :-
    X is Lo + I*H,
    sust(F, X, Fe), Y is abs(Fe),
    (   ( I =:= 0 ; I =:= N ) -> W = 1
    ;   I mod 2 =:= 1         -> W = 4
    ;   W = 2 ),
    Acc1 is Acc + W*Y, I1 is I + 1,
    simpson(F, Lo, H, N, I1, Acc1, S).

%% --- Formato de salida ---

fmt_num(N, S) :- integer(N), !, format(atom(S), '~d', [N]).
fmt_num(N, S) :- rational(N), !, rational(N, P, Q), format(atom(S), '~d/~d', [P, Q]).
fmt_num(N, S) :- float(N), abs(N) < 5.0e-11, !, S = '0'.
fmt_num(N, S) :- float(N), format(atom(S0), '~10f', [N]), recortar(S0, S).

recortar(S0, S) :-
    atom_codes(S0, Cs), reverse(Cs, R0), quitar_ceros(R0, R1),
    reverse(R1, Cs1), atom_codes(S, Cs1).
quitar_ceros([0'0|T], R) :- !, quitar_ceros(T, R).
quitar_ceros([0'.|T], T) :- !.
quitar_ceros(L, L).

fmt(T, Max, S) :-
    fmt_(T, P, S0),
    (   P > Max -> format(atom(S), '(~w)', [S0]) ; S = S0 ).

nombre_salida(atan,  arctan).
nombre_salida(asin,  arcsin).
nombre_salida(acos,  arccos).
nombre_salida(asinh, arcsinh).
nombre_salida(acosh, arccosh).
nombre_salida(atanh, arctanh).

fmt_(N, P, S) :- number(N), !, fmt_num(N, S),
    (   ( N < 0 ; rational(N), \+ integer(N) ) -> P = 400 ; P = 0 ).
fmt_(A, 0, A) :- atom(A), !.
fmt_(log(abs(X)), 0, S) :- !, fmt(X, 999, SX), format(atom(S), 'ln|~w|', [SX]).
fmt_(log(X), 0, S) :- !, fmt(X, 999, SX), format(atom(S), 'ln(~w)', [SX]).
fmt_(A+B, 500, S) :- !, fmt(A, 500, SA), fmt(B, 499, SB), format(atom(S), '~w + ~w', [SA, SB]).
fmt_(A-B, 500, S) :- !, fmt(A, 500, SA), fmt(B, 499, SB), format(atom(S), '~w - ~w', [SA, SB]).
fmt_(A*B, 400, S) :- !, fmt(A, 400, SA), fmt(B, 399, SB), format(atom(S), '~w*~w', [SA, SB]).
fmt_(A/B, 400, S) :- !, fmt(A, 400, SA), fmt(B, 399, SB), format(atom(S), '~w/~w', [SA, SB]).
fmt_(A^B, 200, S) :- !, fmt(A, 199, SA), fmt(B, 200, SB), format(atom(S), '~w^~w', [SA, SB]).
fmt_(-A, 400, S)  :- !, fmt(A, 399, SA), format(atom(S), '-~w', [SA]).
fmt_(sqrt(X), 0, S) :- !, fmt(X, 999, SX), format(atom(S), 'sqrt(~w)', [SX]).
fmt_(T, 0, S) :-
    compound(T), T =.. [F0|Args],
    (   nombre_salida(F0, F) -> true ; F = F0 ),
    maplist(fmt_arg, Args, SArgs),
    atomic_list_concat(SArgs, ', ', Inner),
    format(atom(S), '~w(~w)', [F, Inner]).
fmt_arg(A, S) :- fmt(A, 999, S).

fmt_lista([], '0').
fmt_lista([C-B|R], S) :-
    signo_mag(C, B, Sg, M),
    (   Sg == (-) -> atom_concat('-', M, S0) ; S0 = M ),
    fmt_resto(R, S0, S).

fmt_resto([], S, S).
fmt_resto([C-B|R], Acc, S) :-
    signo_mag(C, B, Sg, M),
    (   Sg == (-) -> Op = ' - ' ; Op = ' + ' ),
    atomic_list_concat([Acc, Op, M], Acc1),
    fmt_resto(R, Acc1, S).

signo_mag(C, B, Sg, M) :-
    (   C < 0 -> Sg = (-) ; Sg = (+) ),
    Ca is abs(C),
    mag(Ca, B, M).

mag(C, B, M) :- C =:= 1, !, fmt(B, 400, M).
mag(C, B, M) :- integer(C), !,
    fmt_num(C, SC), fmt(B, 400, SB), format(atom(M), '~w*~w', [SC, SB]).
mag(C, B, M) :- rational(C), !,
    rational(C, P, Q),
    (   P =:= 1
    ->  fmt(B, 400, SB), format(atom(M), '~w/~d', [SB, Q])
    ;   fmt(B, 400, SB), format(atom(M), '~d*~w/~d', [P, SB, Q])
    ).
mag(C, B, M) :-
    fmt_num(C, SC), fmt(B, 400, SB), format(atom(M), '~w*~w', [SC, SB]).

fmt_aprox(V, S) :- abs(V) < 5.0e-9, !, S = '0'.
fmt_aprox(V, S) :- format(atom(S0), '~8f', [V]), recortar(S0, S).

fmt_valor(V, S) :- rational(V), \+ integer(V), !,
    rational(V, P, Q), VF is float(V), fmt_num(VF, D),
    format(atom(S), '~d/~d (aprox. ~w)', [P, Q, D]).
fmt_valor(V, S) :- fmt_num(V, S).

%% --- Predicado principal ---

resolver(SF, SA, SB, ok(Pares)) :-
    parsear(SF, F),
    parsear_limite(SA, A),
    parsear_limite(SB, B),
    once(integral(F, L0)),
    simplificar(L0, L),
    verificar_dominio(F, A, B),
    evaluar_F(L, B, FB),
    evaluar_F(L, A, FA),
    Area is FB - FA,
    catch(area_geometrica(F, A, B, AG), _, AG = nd),
    fmt(F, 999, FS), fmt_lista(L, IS),
    fmt_valor(FB, FBS), fmt_valor(FA, FAS), fmt_valor(Area, AreaS),
    (   AG == nd -> AGS = 'no disponible' ; fmt_aprox(AG, AGS) ),
    Pares = [funcion=FS, integral=IS, fb=FBS, fa=FAS, area=AreaS, area_abs=AGS].

resolver_seguro(SF, SA, SB, R) :-
    catch(
        call_with_time_limit(10,
            (   resolver(SF, SA, SB, R0) -> R = R0
            ;   R = error('No se pudo resolver la integral.') )),
        E,
        error_a_resultado(E, R)).

error_a_resultado(msg(T), error(T)) :- !.
error_a_resultado(no_soportado(T), error(M)) :- !,
    fmt(T, 999, S),
    format(atom(M), 'No sé integrar el término "~w". Se admiten: constantes, x, x^n (n entero), a^x, e^x, sin, cos, tan, sinh, cosh, tanh, arctan y combinaciones con +, - y constante*función.', [S]).
error_a_resultado(time_limit_exceeded, error('El cálculo tardó demasiado y se canceló.')) :- !.
error_a_resultado(E, error(M)) :-
    format(atom(M), 'No se pudo evaluar la expresión en esos límites (~q).', [E]).

%% --- Punto de entrada (Java) ---

ejecutar :-
    set_stream(user_output, encoding(utf8)),
    current_prolog_flag(argv, Argv),
    (   append(_, [SF, SA, SB], Argv)
    ->  resolver_seguro(SF, SA, SB, R)
    ;   R = error('Faltan argumentos: expresión, límite a y límite b.')
    ),
    imprimir(R),
    flush_output.

imprimir(ok(Pares)) :-
    writeln('OK'),
    forall(member(K=V, Pares), format('~w=~w~n', [K, V])).
imprimir(error(M)) :-
    format('ERROR=~w~n', [M]).