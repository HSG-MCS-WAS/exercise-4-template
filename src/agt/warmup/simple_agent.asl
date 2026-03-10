// simple agent - Warm-up Exercise (not graded)
// Try to implement the plans below, then check simple_agent_solution.asl for the reference solution.

/* Initial rules */

// TODO: Write an inference rule for even(X) using the mod operator
// even(X) :- ...

// TODO: Write an inference rule for odd(X) using negation
// odd(X) :- ...

/* Initial goals */
!start_sum(4,2).
!start_sum(4,-2).
// !start_division(4,2).
// !start_division(4,2.5).
// !start_division(4,0).
// !start_even_or_odd(4).
// !start_even_or_odd(5).
// !start_list_generation(0,4).
// !print_list([0,1,2,3,4]).

/*
 * Plan for reacting to the addition of the goal !start_sum
 * Triggering event: addition of goal !start_sum
 * Context: true (the plan is always applicable)
 * Body: creates the goal of computing the Sum of X and Y
*/
// TODO: Implement a plan for !start_sum(X,Y) that creates a subgoal !compute_sum(Y,X,Sum)
// and then prints X, "+", Y, "=", Sum
// @start_sum_plan
// +!start_sum(X,Y)
//     :   true
//     <-
//         ...
//     .

// TODO: Implement a plan for !compute_sum(X,Y,Sum) that computes Sum = X + Y
// @compute_sum_plan
// +!compute_sum(X,Y,Sum)
//     : true
//     <-
//         ...
//     .

/*
 * Plan for reacting to the addition of the goal !start_division
 * Triggering event: addition of goal !start_division
 * Context: true (the plan is always applicable)
 * Body: creates the goal of computing the division of Dividend by Divisor
 *
 * Uncomment the !start_division goals above after implementing these plans.
*/
// TODO: Implement a plan for !start_division(Dividend,Divisor) that creates a subgoal
// !compute_division(Dividend, Divisor, Quotient) and then prints the result
// @start_division_plan
// +!start_division(Dividend,Divisor)
//     :   true
//     <-
//         ...
//     .

// TODO: Implement a plan for !compute_division that handles division by zero
// (context: Divisor == 0). The plan should print an error message and call .fail
// @compute_division_by_zero_plan
// +!compute_division(Dividend, Divisor, Quotient)
//     : ...
//     <-
//         ...
//     .

// TODO: Implement a plan for !compute_division that computes the quotient
// (context: Divisor \== 0)
// @compute_division_plan
// +!compute_division(Dividend, Divisor, Quotient)
//     : ...
//     <-
//         ...
//     .

// TODO: Implement a failure handling plan -!compute_division that prints an error message.
// Failure plans are triggered with -! when a goal fails.
// @compute_division_failure_plan
// -!compute_division(Dividend,Divisor,_)
//     : true
//     <-
//         ...
//     .

/*
 * Plans for checking if a number is even or odd.
 * Use the inference rules even(X) and odd(X) in the plan context.
 *
 * Uncomment the !start_even_or_odd goals above after implementing these plans.
*/
// TODO: Implement a plan for !start_even_or_odd(X) when X is even
// @start_even_plan
// +!start_even_or_odd(X)
//     :   even(X)
//     <-
//         ...
//     .

// TODO: Implement a plan for !start_even_or_odd(X) when X is odd
// @start_odd_plan
// +!start_even_or_odd(X)
//     : odd(X)
//     <-
//         ...
//     .

// TODO: Implement a failure handling plan for !start_even_or_odd
// @start_even_or_odd_failure_plan
// -!start_even_or_odd(X)
//     :   true
//     <-
//         ...
//     .

/*
 * Plans for generating and printing a list of integers.
 * Use recursion and the .concat internal action.
 *
 * Uncomment the !start_list_generation and !print_list goals above after implementing these plans.
*/
// TODO: Implement a plan for !start_list_generation(Start, End) that creates a subgoal
// !compute_list(Start, End, [], List) and then prints the result
// @start_list_generation_plan
// +!start_list_generation(Start, End)
//     :   true
//     <-
//         ...
//     .

// TODO: Implement the base case for !compute_list — when Current > End, set Result = Acc
// @compute_list_base_plan
// +!compute_list(Current, End, Acc, Result)
//     : Current > End
//     <-
//         ...
//     .

// TODO: Implement the recursive case for !compute_list — when Current <= End,
// use .concat(Acc, [Current], NewAcc) and recurse with Current + 1
// @compute_list_recursive_plan
// +!compute_list(Current, End, Acc, Result)
//     : Current <= End
//     <-
//         ...
//     .

// TODO: Implement a failure handling plan for !compute_list
// -!compute_list(Start, End,_,_)
//     :   true
//     <-
//         ...
//     .

// TODO: Implement a plan for !print_list([]) — the base case that prints
// "All elements have been printed."
// @print_empty_list_plan
// +!print_list([])
//     :   true
//     <-
//         ...
//     .

// TODO: Implement a plan for !print_list([Element | RemainingList]) that prints
// the current element and recurses on the remaining list using the | bar operator
// @print_list_plan
// +!print_list([Element | RemainingList])
//     :   true
//     <-
//         ...
//     .
