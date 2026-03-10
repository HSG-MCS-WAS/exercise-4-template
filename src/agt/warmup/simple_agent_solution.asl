// simple agent - Warm-up Exercise (not graded)
// Solution file — use this as a reference for the graded tasks.

/* Initial rules */
even(X) :- (X mod 2) == 0.
odd(X) :- not even(X).

/* Initial goals */
!start_sum(4,2).
!start_sum(4,-2).
!start_division(4,2).
!start_division(4,2.5).
!start_division(4,0).
!start_even_or_odd(4).
!start_even_or_odd(5).
!start_list_generation(0,4).
!print_list([0,1,2,3,4]).

/*
 * Plan for reacting to the addition of the goal !start_sum
 * Triggering event: addition of goal !start_sum
 * Context: true (the plan is always applicable)
 * Body: creates the goal of computing the Sum of X and Y
*/
@start_sum_plan
+!start_sum(X,Y)
    :   true
    <-
        !compute_sum(Y,X,Sum);
        .print(X, "+", Y, "=", Sum);
    .

@compute_sum_plan
+!compute_sum(X,Y,Sum)
    : true
    <-
        Sum = X + Y;
    .

@start_division_plan
+!start_division(Dividend,Divisor)
    :   true
    <-
        !compute_division(Dividend, Divisor, Quotient);
        .print(Dividend, "/", Divisor, "=", Quotient);
    .

@compute_division_by_zero_plan
+!compute_division(Dividend, Divisor, Quotient)
    : Divisor == 0
    <-
        .print("Division by zero is not possible for ", Dividend, " / ", Divisor);
        .fail;
    .

@compute_division_plan
+!compute_division(Dividend, Divisor, Quotient)
    : Divisor \== 0
    <-
        Quotient = Dividend / Divisor;
    .

@compute_division_failure_plan
-!compute_division(Dividend,Divisor,_)
    : true
    <-
        .print("Unable to compute the division of ", Dividend, " by ", Divisor);
    .

@start_even_plan
+!start_even_or_odd(X)
    :   even(X)
    <-
        .print(X, " is even");
    .

@start_odd_plan
+!start_even_or_odd(X)
    : odd(X)
    <-
        .print(X, " is odd");
    .

@start_even_or_odd_failure_plan
-!start_even_or_odd(X)
    :   true
    <-
        .print("Unable to compute if ", X, " is even or odd");
    .

@start_list_generation_plan
+!start_list_generation(Start, End)
    :   true
    <-
        !compute_list(Start, End, [], List);
        .print("List with integers from ", Start, " to ", End, ": ", List);
    .

@compute_list_base_plan
+!compute_list(Current, End, Acc, Result)
    : Current > End
    <-
        Result = Acc;
    .

@compute_list_recursive_plan
+!compute_list(Current, End, Acc, Result)
    : Current <= End
    <-
        .concat(Acc, [Current], NewAcc);
        !compute_list(Current + 1, End, NewAcc, Result);
    .

-!compute_list(Start, End,_,_)
    :   true
    <-
        .print("Unable to compute a list with integers from ", Start, " to ", End);
    .

@print_empty_list_plan
+!print_list([])
    :   true
    <-
        .print("All elements have been printed.");
    .

@print_list_plan
+!print_list([Element | RemainingList])
    :   true
    <-
        .print("List element: ", Element);
        !print_list(RemainingList);
    .
