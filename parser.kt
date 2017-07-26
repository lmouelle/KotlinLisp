import kotlin.collections.ArrayList


/**
 * Created by lu on 6/5/17.
 *
 *
 * So how would this work? Well I feed in a token stream and then we work with a CFG like grammar
 * Refer to the ANTLR grammar for Clojure and hand build a subset. Which subset? Well it's a lisp thing so
 * just focus on parsing sexps, numbers, fun defs. the stuff currently in [lexer.kt]
 *
 * File -> Form *
 * Form -> atom | collection // TODO implement macros later?
 * collection -> empty | (form*) | [form*] | {form*}
 * atom -> string | number | boolean | func // TODO character, nil, etc
 *
 * Also should I throw exception on parse error or imitate null/maybe/result type with null?
 *
 * and I start to think some of this is better as reader logic, not parser logic
 */

sealed class Expression {
    sealed class Atom : Expression() {
        class Identifier(val value : kotlin.String) : Atom()
        class Number(val value : kotlin.Number) : Atom()
        class Boolean(val value: kotlin.Boolean): Atom()
        sealed class Func : Atom() {
            sealed class Operator : Func() {
                class Plus(val subexpr: Pair<Expression, Expression>) : Operator()
                class Sub(val subexpr: Pair<Expression, Expression>) : Operator()
                class Multiply(val subexpr: Pair<Expression, Expression>) : Operator()
                class Divide(val subexpr: Pair<Expression, Expression>) : Operator()

                class And(val subexpr: Pair<Expression, Expression>) : Operator()
                class Or(val subexpr: Pair<Expression, Expression>) : Operator()

                class LeftGreaterThan(val subexpr: Pair<Expression, Expression>) : Operator()
                class LeftLessThan(val subexpr: Pair<Expression, Expression>) : Operator()
                class EqualTo(val subexpr: Pair<Expression, Expression>) : Operator()
                class Def(val subexpr: Pair<Expression, Expression>) : Operator() // technically not operator I know
            }
            class If(val guard : Expression, val truthy :Expression, val falsy :Expression ): Func()
            class Not(val value : Expression): Func()
        }
    }

    sealed class Collection : Expression() {
        class Map(val forms : kotlin.collections.Collection<Expression>) : Collection()
        class List(val forms: kotlin.collections.Collection<Expression>) : Collection()
        class Vector(val forms: kotlin.collections.Collection<Expression>) : Collection()
    }
}

data class ParseResult(val tokens : List<Token>, val exp : Expression)

fun lookAhead(tokens: List<Token>): Pair<Token, List<Token>> {
    return Pair(tokens.first(), tokens.subList(fromIndex = 0, toIndex = tokens.size - 1))
}

inline fun <reified LeftType, reified RightType> matchCollection(tokens: List<Token>,
                                                                 ctor : (Collection<Expression>) -> Expression) : ParseResult? {
    // TODO see if I can make parserFunc and constructor into type params?
    // Also is there another type safe way/no call to CollType::class way to call the constructor?
    // Right now this uses reflection
    // Alternative is to pass factor/ctor like function
    // I would REALLY like a static resolution to the ctor...
    var (lookahead, remainder) = lookAhead(tokens)
    if (lookahead !is LeftType) return null
    val content = ArrayList<Expression>()
    val top = lookAhead(remainder)
    lookahead = top.first
    remainder = top.second
    while (lookahead !is RightType) {
        val tryForm = matchForm(remainder) // always matchForm, but this needs to be declared before parse form!
        if (tryForm != null) {
            content.add(tryForm.exp)
            remainder = tryForm.tokens
            lookahead = remainder.first()
        } else {
            throw IllegalArgumentException()
        }
    }
    return ParseResult(remainder, ctor(content))
}

fun matchFile(tokens: List<Token>): List<Expression> {
    val stk = ArrayList<Expression>()
    var l = tokens // This list reference needs to change but the contents really should be immutable
    // Like 'const List *  foo'
    while (tokens.any()) {
        val top = matchForm(l)
        if (top == null) {
            throw java.lang.IllegalStateException("Match failed for some exp")
        } else {
            val (remainder, exp) = top
            stk.add(exp)
            l = remainder
        }
    }

    return stk
}

fun matchAtom(tokens: List<Token>) : ParseResult? {
    fun matchBoolean(tokens: List<Token>): ParseResult? {
        val lookahead = tokens.first()
        val rest = tokens.subList(1, tokens.size - 1)
        return when (lookahead) {
            is Token.Atom.Boolean -> ParseResult(rest, Expression.Atom.Boolean(lookahead.value))
            else -> null
        }
    }

    fun matchNumber(tokens: List<Token>): ParseResult? {
        val lookahead = tokens.first()
        val rest = tokens.subList(1, tokens.size - 1)
        return when (lookahead) {
            is Token.Atom.Number -> ParseResult(rest, Expression.Atom.Number(lookahead.value))
            else -> null
        }
    }

    fun matchString(tokens: List<Token>): ParseResult? {
        val lookahead = tokens.first()
        val rest = tokens.subList(1, tokens.size - 1)
        return when (lookahead) {
            is Token.Atom.Identifier -> ParseResult(rest, Expression.Atom.Identifier(lookahead.value))
            else -> null
        }
    }

    fun matchIf(tokens: List<Token>): ParseResult? {
        var tryForm = matchForm(tokens)
        if (tryForm == null) return null
        val (postguard, guard) = tryForm

        tryForm = matchForm(postguard)
        if (tryForm == null) return null
        val (posttruthy, truthy) = tryForm

        tryForm = matchForm(posttruthy)
        if (tryForm == null) return null
        val (postfalsy, falsy) = tryForm

        return ParseResult(postfalsy, Expression.Atom.Func.If(guard, truthy, falsy))

    }

    fun matchNot(tokens: List<Token>): ParseResult? {
        val (first, rest) = lookAhead(tokens)
        if (first !is Token.Operator.Not) return null

        val tryForm = matchForm(rest)
        return if (tryForm == null) { null } else { ParseResult(tryForm.tokens, Expression.Atom.Func.Not(tryForm.exp)) }
    }

    fun matchOperator(tokens: List<Token>): ParseResult? {
        val (first, rest) = lookAhead(tokens)
        if (first !is Token.Operator) return null

        val lhs = matchForm(rest) ?: return null
        val rhs = matchForm(lhs.tokens) ?: return null
        val pair = Pair(lhs.exp, rhs.exp)
        val operator = when(first) {
            is Token.Operator.Add -> Expression.Atom.Func.Operator.Plus(pair)
            is Token.Operator.Subtract -> Expression.Atom.Func.Operator.Sub(pair)
            is Token.Operator.Divide -> Expression.Atom.Func.Operator.Divide(pair)
            is Token.Operator.Multiply -> Expression.Atom.Func.Operator.Multiply(pair)

            is Token.Operator.And -> Expression.Atom.Func.Operator.And(pair)
            is Token.Operator.Or -> Expression.Atom.Func.Operator.Or(pair)

            is Token.Operator.LeftGreaterThan -> Expression.Atom.Func.Operator.LeftGreaterThan(pair)
            is Token.Operator.LeftLessThan -> Expression.Atom.Func.Operator.LeftLessThan(pair)
            is Token.Operator.EqualTo -> Expression.Atom.Func.Operator.EqualTo(pair)
            is Token.Operator.Def -> Expression.Atom.Func.Operator.Def(pair)
        }
        return ParseResult(rhs.tokens, operator)

    }

    val ret = matchBoolean(tokens)
    if (ret != null) {
        return ret
    }
    val ret2 = matchNumber(tokens)
    if (ret2 != null) {
        return ret2
    }

    val ret3 = matchString(tokens)
    if (ret3 != null) {
        return ret3
    }

    val ret4 = matchNot(tokens)
    if (ret4 != null) {
        return ret4
    }

    val ret5 = matchIf(tokens)
    if (ret5 != null) {
        return ret5
    }

    val ret6 = matchOperator(tokens)
    if (ret6 != null) {
        return ret6
    }
    return null
}

fun matchForm(tokens: List<Token>): ParseResult? {
    fun parseList(tokens: List<Token>) : ParseResult? {
        return matchCollection<Token.Delimiter.LeftParan, Token.Delimiter.RightParan>(tokens, {s -> Expression.Collection.List(s)})
    }

    fun parseMap(tokens: List<Token>): ParseResult? {
        return matchCollection<Token.Delimiter.LeftBrace, Token.Delimiter.RightBrace>(tokens, { s -> Expression.Collection.Map(s)} )
    }

    fun parseVector(tokens: List<Token>): ParseResult? {
        return matchCollection<Token.Delimiter.LeftBracket, Token.Delimiter.RightBrace>(tokens, {s -> Expression.Collection.Vector(s)})
    }

    val result = matchAtom(tokens)
    if (result != null) {
        return result
    }
    val tryList = parseList(tokens)
    if (tryList != null) {
        return tryList
    }
    val tryMap = parseMap(tokens)
    if (tryMap != null) {
        return tryMap
    }
    val tryVec = parseVector(tokens)
    if (tryVec != null) {
        return tryVec
    }
    return null
}



