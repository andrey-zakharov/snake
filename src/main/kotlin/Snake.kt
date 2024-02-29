import org.openrndr.extra.noise.random
import kotlin.math.roundToInt

// namespace snake game
data class GameSession(
    var moveDir: Dir = Dir.UP,
    var lives: Int = 1,
    // flip flop
    var space: SnakeField
) {

    // read state
    // write state
    // write counter
    fun makeTurn(delta: Double): Boolean {

        // run cell kernel
        // save state to apply
        val turnCommands = mutableListOf<SnakeField.Cell>()
        var res = true

        // ai turn
        // it needs
        // 1.observation
        // 2.decision
        // 3.action
        this.space.currentHead?.let { aiTurn( coords( it.x, it.y ) ) }

        // cell automata
        space.forEach { data ->

            val newState = data.state.visitTurn?.let { it( coords(data.x, data.y), this@GameSession ) } ?: data.state

            val newCounter: Short = when( newState ) {
                SnakeCellType.STONE,
                SnakeCellType.RABBIT,
                SnakeCellType.HEAD -> 0
                SnakeCellType.TAIL -> (data.counter + 1).toShort()
                SnakeCellType.EMPTY -> {

                    val nmn = Dir.values().map { coords( data.x + it.offsets[0], data.y + it.offsets[1] ) }
                    val cn = nmn.map { space.getCell(it) }

                    if ( cn.any { it.state == SnakeCellType.RABBIT } ) {
                        1
                    } else {
                        cn
                            .filter { it.state == SnakeCellType.EMPTY && it.counter > 0 }
                            .minOfOrNull { it.counter + 1 }?.toShort() ?:
                        0
                    }
                }
            }

            // compare and sets
            if ( data.state == newState && data.counter == newCounter ) {
                return@forEach
            }

            // fire event
            turnCommands.add( SnakeField.Cell( newState, newCounter, data.x, data.y ) )

            // special case  rabbit
            if ( data.state == SnakeCellType.RABBIT && newState == SnakeCellType.HEAD ) {
                lives ++
                setRabbit()
            }

            if ( (data.state == SnakeCellType.TAIL || data.state == SnakeCellType.STONE) && newState == SnakeCellType.HEAD ) {
                // game over
                // gameOver()
                lives = 1
                res = false
            }
        }

        // apply after step
        turnCommands.forEach { op ->
            space.setCell(op)
        }

        return res
    }

    fun aiTurn(pos: Coords) {

    }
}

class SnakeField(val width: Int = 20, val height: Int = 20, val rabbitRadius: Int = 1) {

    val middle get() = listOf(width/2.0, height/2.0)
    var currentHead: Cell? = null//emptyCell()

    private val toIndex = storageBounds(width, height)

    interface CellData {
        val x: Int
        val y: Int
        val state: SnakeCellType
        val counter: Short
    }
    data class Cell(
        override val state: SnakeCellType,
        override val counter: Short = 0,
        override val x: Int = 0,
        override val y: Int = 0,
    ) : CellData {
        companion object {
            val EMPTY = Cell(state = SnakeCellType.EMPTY, counter = 0)
        }
    }

    private val states = Array(width * height) { SnakeCellType.EMPTY }
    private val counter = Array<Short>(width * height) { 0 }

    // cells
    fun forEach(fn: SnakeField.(cell: CellData) -> Unit ) {

        // by axis
        (0 until width).forEach { x ->
            (0 until height).forEach { y ->
                val i = toIndex(x, y)
                fn(Cell (state = states[ i ], counter = counter[ i ], x = x, y = y))
            }
        }
    }
    fun setCell(cell: Cell) {
        val idx = toIndex(cell.x, cell.y)
        states[ idx ] = cell.state
        counter[ idx ] = cell.counter

        // for ai robust
        if ( cell.state == SnakeCellType.HEAD ) {
            currentHead = cell
        }
    }

    fun getCell(key: Coords): Cell {
        val idx = toIndex(key[0], key[1])
        return Cell(
            states[ idx ],
            counter[ idx ],
            key[0], key[1]
        )
    }


    fun getState(coords: Coords): SnakeCellType = getState(coords[0], coords[1])
    fun getState(x: Int, y: Int) = states[ toIndex(x, y) ]
    fun setState(coords: Coords, state: SnakeCellType) { states[ toIndex(coords[0], coords[1]) ] = state }

    fun decCounter(coords: Coords): Boolean { return counter[ toIndex(coords[0], coords[1]) ]-- >= 0 }
    fun setCounter(coords: Coords, v: Short) { counter[ toIndex(coords[0], coords[1]) ] = v }
    fun getCounter(coords: Coords) = getCounter(coords[0], coords[1])
    fun getCounter(x: Int, y: Int) = counter[ toIndex(x, y) ]

    fun setRabbit() {
        // clear current rabbit, or assert its already
        // space take axes, currently x and y
        var found = false

        while ( true ) {
            val randomCoords = coords( (0 .. 1).map {
                val max = when( it ) { 0 -> width; 1 -> height; else -> -1 }
                random(0.0, max.toDouble()).roundToInt()
            } )

            if ( this.isEmpty( randomCoords ) ) {
                //grid.drawCircle( randomCoords, rabbitRadius )
                setState( randomCoords, SnakeCellType.RABBIT )

                break
            }
        }
    }

    fun isEmpty( coords: Coords ) =
        coords[0] >= 0.0 && coords[0] < width &&
                coords[1] >= 0.0 && coords[1] < height &&
                getState(coords) == SnakeCellType.EMPTY


}

// RULES
fun checkIncomingHead(pos: Coords, state: GameSession) =
    state.space.getState(pos.withNegativeOffset(state.moveDir)) == SnakeCellType.HEAD
enum class SnakeCellType(val visitTurn: ((pos: Coords, state: GameSession) -> SnakeCellType)? = null) {
    EMPTY({ pos, state ->
        if ( checkIncomingHead(pos, state) ) {
            HEAD
        } else {
            EMPTY
        }
    }),
    STONE,
    HEAD( { _, _ ->
        // moves forward
        // this.cell.counter = prevState.lives
        // side effect
        TAIL
    }),
    TAIL( { pos, state ->
        if ( checkIncomingHead(pos, state) ) {
            //end of game
            HEAD
        } else if ( state.space.getCounter( pos ) >= state.lives  ) {
            EMPTY
        } else {
            TAIL
        }
    } ),
    RABBIT( { pos, state ->
        if ( checkIncomingHead(pos, state) ) {
            //onEat
            HEAD
        } else {
            RABBIT
        }
    })
    ;


}

enum class GameAction() {
    MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT
}
