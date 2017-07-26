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
        object RightBracket: Delimiter("\\]".toRegex())
        object LeftBrace: Delimiter("\\{".toRegex())
        object RightBrace: Delimiter("\\}".toRegex())
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
        object Subtract: Operator("\\-".toRegex())
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
            "\\z".toRegex() to {_ -> Token.EOF}
    )
    return mappings
}

fun tokenize(input: String): Collection<Token> {
    fun _tokenize(input: String, index: Int, accumulator : List<Token>) : Collection<Token> {
        return if (index >= input.length) {
            accumulator
        } else {
            for ((pattern, constructor) in getAllTypes()) {
                val match = pattern.find(input, index)
                if (null != match) {
                    return _tokenize(input, match.value.length + index, accumulator + constructor.invoke(match.value))
                }
            }
            throw IllegalArgumentException("No match found")
        }
    }
    val segments = input.split(Token.Whitespace.regexPattern) // recursive isn't it?
    return segments.flatMap { _tokenize(it, 0, ArrayList()) }
}