/**
 * Created by lu on 6/5/17.
 *
 *
 * So how would this work? Well I feed in a token stream and then we work with a CFG like grammar
 * Refer to the ANTLR grammar for Clojure and hand build a subset. Which subset? Well it's a lisp thing so
 * just focus on parsing sexps, numbers, fun defs. the stuff currently in [lexer.kt]
 *
 * File -> Form *
 * Form -> literal | list | vector // TODO implement macros and maps later
 * List -> (form*)
 * vector -> [form*]
 * literal -> string | number | boolean // TODO character, nil, etc
 */

