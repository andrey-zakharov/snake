
/**
 * decimal digits to 7 segments
 *  1
 * 2 3
 *  4
 * 5 6
 *  7
 */
// better to just array
enum class DecimalDigits(val template: List<Int>) {
    zero( listOf( 1, 2, 3, 5, 6, 7 ) ),
    one( listOf(3, 6) ),
    two( listOf( 1, 3, 4, 5, 7 ) ),
    three( listOf ( 1, 3, 4, 6, 7) ),
    four( listOf ( 2, 3, 4, 6) ),

    five( listOf ( 1, 2, 4, 6, 7) ),
    six( listOf ( 1, 2, 4, 5, 6, 7) ),
    seven( listOf ( 1, 3, 6 ) ),
    eight( listOf ( 1, 2, 3, 4, 5, 6, 7 ) ),
    nine( listOf ( 1, 2, 3, 4, 6, 7 ) ),
    // or as binary one byte
    ;

    init {
        //val byte = template.foldRight( 0 ) { v, r -> r + 1.shl(v-1) }
        //println( "${template.joinToString(", ")} = $byte")
    }
    fun asVertices() = asPoints( template )

    companion object {
        val digits = arrayOf( zero, one, two, three, four, five, six, seven, eight, nine )

        fun asPoints(list: List<Int>) = list.flatMap { asPoint(it) }

    }
}

fun asPoint(digitSegment: Int): List<Coords> = when(digitSegment) {
    1 -> listOf(  coords(1, 0) )
    2 -> listOf( coords( 0, 1 ) ); 3 -> listOf( coords( 2, 1 ) )
    4 -> listOf( coords( 1, 2 ) )
    5 -> listOf( coords( 0, 3 ) ); 6 -> listOf( coords( 2, 3 ) )
    7 -> listOf( coords( 1, 4 ) )
    else -> throw Error(digitSegment.toString())
}

/*
  0123
0 .--.
1 |..|
2 |..|
3 .--.
4 |..|
5 |..|
6 .--.
 */
fun lineDigits(digitSegment: Int) = when(digitSegment) {
    1 -> listOf( coords(1, 0 ), coords(2, 0 ) )
    2 -> listOf( coords( 0, 1 ), coords( 0, 2 ) );
    3 -> listOf( coords( 3, 1 ), coords( 3, 2 ) )
    4 -> listOf( coords( 1, 3 ), coords( 2, 3 ) )
    5 -> listOf( coords( 0, 4 ), coords( 0, 5 ) ); 6 -> listOf( coords( 3, 4 ), coords( 3, 5 ) )
    7 -> listOf( coords( 1, 6 ), coords( 2, 6 ) )
    else -> throw Error(digitSegment.toString())
}

val minusSign = listOf( 4 )
val signOffset = coords( 5, 0 ) as Coords

fun Int.drawAsDigit(offset: Coords, verticesProvider: (digit: Int) -> List<Coords>): List<Coords> {
    if ( this < 0 ) {
        return minusSign.flatMap { verticesProvider(it) } +
                ( - this ).drawAsDigit( signOffset, verticesProvider )
    }
    if ( this < 10 ) {
        // reduce
        return DecimalDigits.digits[ this ].template.flatMap { verticesProvider(it).map { vert ->
            coords( vert[0] + offset[0], vert[1] + offset[1] )
        } }
    }

    var remainder = this
    val stack = ArrayDeque<Int>( 2 )
    while ( remainder > 0 ) {
        val one = remainder % 10
        stack += one
        remainder -= one
        remainder /= 10
    }

    stack.reverse()
    // direct  flow

    return stack.flatMapIndexed { index, i ->
        val off = coords( signOffset[0] * index, signOffset[1] * index )
        i.drawAsDigit( off, verticesProvider )
    }
}