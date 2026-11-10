% Pruebas automaticas de la base de conocimiento.
% Ejecutar (desde esta carpeta):   swipl -g correr -t halt pruebas.pl
:- ['../src/main/resources/calculo.pl'].

% caso(Expr, A, B, IntegralEsperada, AreaEsperada)
caso('5',              '0', '2',  '5*x',                     10).
caso('x',              '0', '4',  'x^2/2',                   8).
caso('3*x',            '0', '2',  '3*x^2/2',                 6).
caso('x^2',            '0', '3',  'x^3/3',                   9).
caso('x^(-2)',         '1', '2',  '-x^(-1)',                 1/2).
caso('x^(-1)',         '1', 'e',  'ln|x|',                   1).
caso('1/x',            '1', '2',  'ln|x|',                   log(2)).
caso('3*x^2',          '0', '2',  'x^3',                     8).
caso('x^2 - 3*x + 2',  '0', '3',  'x^3/3 - 3*x^2/2 + 2*x',   3/2).
caso('x^2/2',          '0', '3',  'x^3/6',                   9/2).
caso('-x',             '0', '2',  '-x^2/2',                  -2).
caso('2*x + 2*x',      '0', '1',  '2*x^2',                   2).
caso('x - x',          '0', '1',  '0',                       0).
caso('sin(x)',         '0', 'pi', '-cos(x)',                 2).
caso('cos(x)',         '0', 'pi/2', 'sin(x)',                1).
caso('x^2 + sin(x)',   '0', 'pi', 'x^3/3 - cos(x)',          pi^3/3 + 2).
caso('sinh(x)',        '0', '1',  'cosh(x)',                 cosh(1) - 1).
caso('sh(x)',          '0', '1',  'cosh(x)',                 cosh(1) - 1).
caso('cosh(x)',        '0', '1',  'sinh(x)',                 sinh(1)).
caso('2^x',            '0', '3',  '2^x/ln(2)',               7/log(2)).
caso('e^x',            '0', '1',  'e^x',                     e - 1).
caso('tan(x)',         '0', '1',  '-ln|cos(x)|',             -log(cos(1))).
caso('tan(x)',         '2', '3',  '-ln|cos(x)|',             -log(abs(cos(3))) + log(abs(cos(2)))).
caso('tg(x)',         '-1', '1',  '-ln|cos(x)|',             0).
caso('tan(x) + x',     '0', '1',  '-ln|cos(x)| + x^2/2',     -log(cos(1)) + 1/2).
caso('atan(x)',        '0', '1',  'x*arctan(x) - ln(1 + x^2)/2', pi/4 - log(2)/2).
caso('arctan(x)',     '-1', '1',  'x*arctan(x) - ln(1 + x^2)/2', 0).
caso('3*atan(x)',      '0', '2',  '3*x*arctan(x) - 3*ln(1 + x^2)/2', 3*(2*atan(2) - log(5)/2)).
caso('tanh(x)',        '0', '1',  'ln(cosh(x))',             log(cosh(1))).
caso('2*tanh(x) + x',  '0', '2',  '2*ln(cosh(x)) + x^2/2',   2*log(cosh(2)) + 2).
caso('th(x) - sh(x)',  '0', '1',  'ln(cosh(x)) - cosh(x)',   log(cosh(1)) - cosh(1) + 1).
caso('exp(x)',         '0', '1',  'e^x',                     e - 1).
caso('3*2^x',          '0', '1',  '3*2^x/ln(2)',             3/log(2)).
caso('x^3 + 4*x',      '2', '0',  'x^4/4 + 2*x^2',           -12).    % limites invertidos
caso('2.5*x',          '0', '2',  '1.25*x^2',                5.0).
caso('pi*x',           '0', '1',  '1.5707963268*x^2',        pi/2).
caso('X**2',           '0', '3',  'x^3/3',                   9).      % mayusculas y **

% error(Expr, A, B): debe devolver error
error('x*x',     '0', '1').
error('sin(2*x)','0', '1').
error('x^(1/2)', '0', '1').
error('x^x',     '0', '1').
error('x^(-2)', '-1', '1').          % contiene x=0
error('1/x',    '-1', '2').
error('3*x^',    '0', '1').
error('x + y',   '0', '1').
error('x',       'a', '1').
error('(-1)^x',  '0', '1').
error('tan(x)',  '0', 'pi/2').           % asintota en el limite
error('tan(x)',  '1', '2').              % contiene pi/2
error('tan(x)',  '0', '3*pi/2').
error('tan(x)/x', '1', '2').
error('atan(2*x)', '0', '1').

% Integracion numerica independiente (punto medio) para validar F(b)-F(a)
medio(F, A, B, V) :-
    N = 40000, H is (B - A)/N,
    numlist(1, N, Is),
    foldl([I, S0, S]>>( X is A + (I - 0.5)*H, sust(F, X, Fe), S is S0 + Fe*H ), Is, 0.0, V).

correr :-
    findall(ok, (caso(E,A,B,I,Esp), prueba_ok(E,A,B,I,Esp)), Oks),
    findall(x,  caso(_,_,_,_,_), Todos),
    length(Oks, NO), length(Todos, NT),
    format('~nCasos correctos: ~d de ~d~n', [NO, NT]),
    findall(ok, (error(E,A,B), prueba_error(E,A,B)), Es),
    findall(x,  error(_,_,_), TodosE),
    length(Es, NE), length(TodosE, NTE),
    format('Errores detectados: ~d de ~d~n', [NE, NTE]),
    (   NO =:= NT, NE =:= NTE -> writeln('TODO OK') ; writeln('HAY FALLOS'), halt(1) ).

prueba_ok(E, A, B, I, Esp) :-
    resolver_seguro(E, A, B, R),
    (   R = ok(P) -> true ; format('FALLO ~w [~w,~w]: ~w~n', [E,A,B,R]), fail ),
    member(integral=IS, P), member(area=AS, P),
    E1 is Esp, fmt_valor(E1, EspS),
    (   IS == I -> true ; format('FALLO integral ~w: obtuve ~w, esperaba ~w~n', [E,IS,I]), fail ),
    (   AS == EspS -> true
    ;   area_aprox(AS, E1) -> true
    ;   format('FALLO area ~w: obtuve ~w, esperaba ~w~n', [E,AS,EspS]), fail ),
    % validacion cruzada con integracion numerica
    parsear(E, F), parsear_limite(A, A1), parsear_limite(B, B1),
    medio(F, float(A1), float(B1), Num0), Num = Num0,
    Ref is float(E1),
    (   abs(Num - Ref) < 1.0e-4*max(1, abs(Ref)) -> true
    ;   format('FALLO validacion numerica ~w: ~w vs ~w~n', [E, Num, Ref]), fail ).

% compara con tolerancia usando el decimal de "p/q (aprox. d)" o el numero directo
area_aprox(AS, Esp) :-
    atomic_list_concat(Partes, ' ', AS), last(Partes, U0),
    atomic_list_concat(L, ')', U0), L = [U|_],
    atom_number(U, V),
    abs(V - float(Esp)) < 1.0e-8*max(1, abs(float(Esp))).

prueba_error(E, A, B) :-
    resolver_seguro(E, A, B, R),
    (   R = error(M) -> format('  ok  ~w [~w,~w] -> ~w~n', [E,A,B,M])
    ;   format('FALLO: ~w [~w,~w] deberia dar error~n', [E,A,B]), fail ).
