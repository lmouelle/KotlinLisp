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
    class Number(val value : kotlin.Number) : Token("[0-9]+".toRegex())
    class String(val value: kotlin.String): Token("[a-zA-Z][\\w]*".toRegex())
    /* Chars that need balancing */
    object LeftParan : Token("\\(".toRegex())
    object RightParan: Token("\\)".toRegex())
    object LeftBracket: Token("\\[".toRegex())
    object RightBracket: Token("\\]".toRegex())
    object LeftBrace: Token("\\{".toRegex())
    object RightBrace: Token("\\}".toRegex())
    /* Special forms */
    object If: Token("if".toRegex())
    object False : Token("false".toRegex())
    object True : Token("true".toRegex())
    object Def: Token("def".toRegex())
    /* Bool ops */
    object And: Token("and".toRegex())
    object Or: Token("or".toRegex())
    object LeftGreaterThan: Token(">".toRegex())
    object LeftLessThan: Token("<".toRegex())
    object EqualTo : Token("=".toRegex())// Structural equality
    object Not: Token("not".toRegex())
    /* Num ops */
    object Add: Token("\\+".toRegex())
    object Subtract: Token("\\-".toRegex())
    object Divide: Token("/".toRegex())
    object Multiply: Token("\\*".toRegex())
    /* etc */
    object EOF: Token("\\z".toRegex())
    object Whitespace : Token("[ \t\r\n,]".toRegex())
}

fun getAllTypes(): Map<Regex, (String) -> Token> {
    val mappings = mapOf<Regex, (String) -> Token> (
            "and".toRegex() to {_ :String -> Token.And},
            "[0-9]+".toRegex() to {s -> Token.Number(s.toInt())},
            "[a-zA-Z][\\w]*".toRegex() to {s -> Token.String(s)},
            "\\(".toRegex() to {_ -> Token.LeftParan},
            "\\)".toRegex() to {_ -> Token.RightParan},
            "\\[".toRegex() to {_ -> Token.LeftBracket},
            "]".toRegex() to { _ -> Token.RightBracket},
            "\\{".toRegex() to {_ -> Token.LeftBrace},
            "}".toRegex() to { _ -> Token.RightBrace},
            "if".toRegex() to {_ -> Token.If},
            "false".toRegex() to {_ -> Token.False},
            "true".toRegex() to {_ -> Token.True},
            "def".toRegex() to {_ -> Token.Def},
            "and".toRegex() to {_ -> Token.And},
            "or".toRegex() to {_ -> Token.Or},
            ">".toRegex() to {_ -> Token.LeftGreaterThan},
            "<".toRegex() to  {_ -> Token.LeftLessThan},
            "=".toRegex() to {_ -> Token.EqualTo},
            "not".toRegex() to {_ -> Token.Not},
            "\\+".toRegex() to {_ -> Token.Add},
            "-".toRegex() to { _ -> Token.Subtract},
            "/".toRegex() to {_ -> Token.Divide},
            "\\*".toRegex() to {_ -> Token.Multiply},
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