interface Vectorable {
    operator fun plus(sval: Vectorable): Vectorable
}

abstract class Grid<Key : Vectorable, Cell>  {
    abstract operator fun get(pos: Key): Cell

    // 2d grid
//    fun right(origin: Key) = get(origin + Dir.RIGHT)
//    fun down(origin: Key) = get(origin.withOffset(Dir.DOWN))
//    fun left(origin: Key) = get(origin.withOffset(Dir.LEFT))
//    fun up(origin: Key) = get(origin.withOffset(Dir.UP))
    fun moore(origin: Key): Iterable<Key> {
        TODO("Not yet implemented")
    }

    // is order defined as left, up right, down
    fun<O> neumann(origin: Pos, order: Iterable<O>): Iterable<Pos> {
        return Dir.values().map { d -> origin.withOffset(d) }
    }

    // kernel  for each cell
    abstract fun every(block: (x: Cell) -> Unit)

    //{

    // very bad impl
    /*fun forEach

        // by  axis
        (0 until width).forEach { x ->
            (0 until height).forEach { y ->
                block(cell(x, y, get(x, y)))
            }

        }
    }*/
}

// axis

typealias Coords = Array<Int>
fun coords(x: Int, y: Int): Coords = arrayOf(x, y)
fun coords(c: List<Int>): Coords = c.toTypedArray()

// it needs xyz and vector math.
val List<Double>.coords get() = map { it.toInt() }.toTypedArray()

fun Coords.withOffset(dir: Dir) = mapIndexed { index: Int, c: Int -> c + dir.offsets[index] }

fun Coords.withNegativeOffset(dir: Dir) = mapIndexed { index: Int, c: Int -> c - dir.offsets[index] }.toTypedArray()

value class Pos(val coords: Coords) {

//    constructor(vararg coord: Int, ) = Pos(listOf(*coord))
    //2d
    val x get() = coords[0]
    val y get() = coords[1]

    // TBD cache
    fun withOffset(dir: Dir) =
        Pos( coords.mapIndexed { index: Int, c: Int -> c + dir.offsets[index] }.toTypedArray() )
    fun withNegativeOffset(dir: Dir) =
        coords.mapIndexed { index: Int, c: Int -> c - dir.offsets[index] }
}

data class BoundingBox( val min: Pos, val max: Pos )

enum class Dir(val offsets: Coords) {
    RIGHT(coords( 1, 0 ) ),
    DOWN(coords( 0,-1,) ),
    LEFT(coords( -1, 0,) ),
    UP(coords( 0, 1 ) );
    val reversed: Dir get() {
        val s = Dir.values().size
        val sh = s / 2
        return Dir.values()[ (this.ordinal + sh).mod(s) ]
    }/*  when(this) {
        RIGHT -> LEFT
        DOWN -> UP
        LEFT -> RIGHT
        UP -> DOWN
    }*/
}

// TBD tests
fun Pos.toStorageIndex(bb: BoundingBox) = toStorageIndex( bb.max.x, bb.max.y, bb.min.x, bb.min.y )
fun Pos.toStorageIndex(width: Int, height: Int, originX: Int = 0, originY: Int =0 ) = (y-originY).mod(height) * width + (x-originX).mod(width)
fun Coords.toStorageIndex(width: Int, height: Int, originX: Int = 0, originY: Int =0 ): Int = toStorageIndex(get(0), get(1), originX, originY)

fun toStorageIndex(x: Int, y: Int, width: Int, height: Int, originX: Int = 0, originY: Int =0): Int = (x-originX).mod(width) + (y-originY).mod(height) * width
fun Int.fromStorageIndex(width: Int, height: Int, originX: Int = 0, originY: Int =0) = Pos(coords(
    this.mod(width), this / width
))

fun storageBounds( width: Int, height: Int ): ( Int, Int ) -> Int {
    return { x, y ->
        toStorageIndex(x, y, width, height)
    }
}

/// for 2d
/*
fun <STATE : Enum<STATE>>enumGrid(width: Int, height: Int,
                                  stateDeserialize: (s: Byte) -> STATE
) = EnumGrid(width, height, stateDeserialize)
*/

// GridCellAccessor ?
// абстрагирован он хранения
// я хочу правила, работающие и для hexgrid.
/*
class EnumGrid<STATE : Enum<STATE>>(
    val width: Int = 20, val height: Int = 20,
    val stateDeserialize: (s: Byte) -> STATE,
    val stateSerialize: (s: STATE) -> Byte = { it.ordinal.toByte() }
): Grid<STATE>() {

    val states = ByteArray(width * height )
    //override fun set(pos: Pos, cell: STATE) = set(pos.x, pos.y, cell)


    operator fun get(coords: Coords) = get(coords[0], coords[1])
    fun get(x: Int, y: Int): STATE = stateDeserialize(states[ y.mod(height) * width + x.mod(width) ])

    fun set(coords: List<Int>, state: STATE) = set( coords[0], coords[1], state)
    fun set(x: Int, y: Int, state: STATE) { states[ y.mod(height) * width + x.mod(width) ] = stateSerialize(state) }



}
*/

