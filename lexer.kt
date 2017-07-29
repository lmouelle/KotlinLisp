/**
 * Created by lu on 5/28/17.
 */

sealed class Token(val regexPattern : Regex) {
    /*  Let's start with just a subset of Clojure
        Every type should have an associated regex pattern,
        So [ is left bracket, and it would be...well "[".
        Number would be more like 0-9* I guess

        One thing is that the tokens are associated with data like regex,
        but that's all static. It does not change with runtime instances.
        A few values do change at runtime, but the other tokens deserve to be objects/singletons then

        hm maybe this should be an enum class instead?
    */

    /* Values */
    sealed class Atom(val p: Regex) : Token(p) {
        class Number(val value : kotlin.Number) : Atom("[0-9]+".toRegex())
        class Identifier(val value: kotlin.String): Atom("[a-zA-Z][\\w]*".toRegex())
        class Boolean(val value : kotlin.Boolean) : Atom("true|false".toRegex())
    }
    sealed class Delimiter(val p : Regex) : Token(p) {
        object LeftParan : Delimiter("\\(".toRegex())
        object RightParan: Delimiter("\\)".toRegex())
        object LeftBracket: Delimiter("\\[".toRegex())
        object RightBracket: Delimiter("]".toRegex())
        object LeftBrace: Delimiter("\\{".toRegex())
        object RightBrace: Delimiter("}".toRegex())
    }

    sealed class Operator(val p : Regex) : Token(p) {
        /* Bool ops */
        object And: Operator("and".toRegex())
        object Or: Operator("or".toRegex())

        object LeftGreaterThan: Operator(">".toRegex())
        object LeftLessThan: Operator("<".toRegex())
        object EqualTo : Operator("=".toRegex())// Structural equality
        /* Num ops */
        object Add: Operator("\\+".toRegex())
        object Divide: Operator("/".toRegex())
        object Subtract: Operator("-".toRegex())
        object Multiply: Operator("\\*".toRegex())
        /* Special forms */
        object If: Token("if".toRegex())
        object Def: Token("def".toRegex())
        object Not: Token("not".toRegex())

    }

    /* etc */
    object EOF: Token("\\z".toRegex())
    object Whitespace : Token("[ \t\r\n,]".toRegex())
}

fun getAllTypes(): Map<Regex, (String) -> Token> {
    val mappings = mapOf<Regex, (String) -> Token> (
            "and".toRegex() to {_ :String -> Token.Operator.And },
            "[0-9]+".toRegex() to {s -> Token.Atom.Number(s.toInt())},
            "[a-zA-Z][\\w]*".toRegex() to {s -> Token.Atom.Identifier(s)},
            "\\(".toRegex() to {_ -> Token.Delimiter.LeftParan },
            "\\)".toRegex() to {_ -> Token.Delimiter.RightParan },
            "\\[".toRegex() to {_ -> Token.Delimiter.LeftBracket },
            "]".toRegex() to { _ -> Token.Delimiter.RightBracket },
            "\\{".toRegex() to {_ -> Token.Delimiter.LeftBrace },
            "}".toRegex() to { _ -> Token.Delimiter.RightBrace },
            "if".toRegex() to {_ -> Token.Operator.If },
            "false|true".toRegex() to {s -> Token.Atom.Boolean(s.toBoolean())},
            "def".toRegex() to {_ -> Token.Operator.Def },
            "and".toRegex() to {_ -> Token.Operator.And },
            "or".toRegex() to {_ -> Token.Operator.Or },
            ">".toRegex() to {_ -> Token.Operator.LeftGreaterThan },
            "<".toRegex() to  {_ -> Token.Operator.LeftLessThan },
            "=".toRegex() to {_ -> Token.Operator.EqualTo },
            "not".toRegex() to {_ -> Token.Operator.Not },
            "\\+".toRegex() to {_ -> Token.Operator.Add },
            "-".toRegex() to { _ -> Token.Operator.Subtract },
            "/".toRegex() to {_ -> Token.Operator.Divide },
            "\\*".toRegex() to {_ -> Token.Operator.Multiply },
            "\\z".toRegex() to {_ -> Token.EOF},
            "[ \t\n\r]".toRegex() to {_ -> Token.Whitespace}
    )
    return mappings
}

fun findTokenMatch(string: String): Pair<Token, Int> {
    // find longest match
    var candidate : Token? = null
    var candidateRange = IntRange.EMPTY
    var retlen = 0
    for ((pattern, ctor) in getAllTypes()) {
        val result = pattern.find(string)
        if (result != null) {
            val matchlen = result.value.length
            if (candidate == null || matchlen > retlen) {
                candidate = ctor(result.value)
                retlen = matchlen
            }
        }
    }
    if (candidate == null) throw IllegalArgumentException("No match found for string")
    return Pair(candidate, retlen)
}

fun tokenize(input: String): Collection<Token> {
    // Try to match the string until I see EOF
    val tokensCollected = ArrayList<Token>()
    var (tok, matchLength) = findTokenMatch(input)
    var file = input
    while (tok !is Token.EOF) {
        tokensCollected.add(tok)
        file = file.substring(matchLength)
        val t = findTokenMatch(file)
        tok = t.first
        matchLength = t.second
    }
    return tokensCollected
}