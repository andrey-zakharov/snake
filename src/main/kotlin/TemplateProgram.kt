import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.color.rgb
import org.openrndr.draw.*
import org.openrndr.extra.color.palettes.colorSequence
import org.openrndr.extra.noise.random

import org.openrndr.math.Vector2
import kotlin.math.*


const val AR_LEFT =  37
const val AR_UP =  38
const val AR_RIGHT =  39
const val AR_DOWN =  40

fun calcCellWidth(windowWidth: Int, windowHeight: Int, fieldWidth: Int, fieldHeight: Int): Float {
    return min( windowWidth.toFloat() / fieldWidth.toFloat(), windowHeight.toFloat() / fieldHeight.toFloat() )
}


suspend fun main() = applicationAsync {
    program {

        var x = 0.0
        var y = 0.0

        var lastTurnSecs = 0.0

        val bg = makeBackground()

//        val field = enumGrid(20, 20) {
//            s -> SnakeCellType.values()[s.toInt()]
//        }
//
//        // game init
//        field.set( field.middle, SnakeCellType.HEAD )
//        field.setRabbit()
        val field = SnakeField( 25, 25, 4 )
        //set level
        field.setState( field.middle.coords, SnakeCellType.HEAD )
        ( field.middle[0].toInt() - 2 .. field.middle[0].toInt() + 2 ).forEach {
            field.setState( coords(it, field.middle[1].toInt()-1), SnakeCellType.STONE)
        }
        //field.setCounter( field.middle.coords, 1 )
        field.setRabbit()

        var speedHz = 5.0
        val gameState = GameSession(space = field)


        fun changeDir(toDir: Dir) {
            // prevent opposite
            if ( toDir == gameState.moveDir.reversed ) return
            gameState.moveDir = toDir
        }

        keyboard.keyDown.listen { ev ->
            when(ev.key) {

                AR_UP -> changeDir( Dir.UP )
                AR_DOWN -> changeDir( Dir.DOWN )
                AR_RIGHT -> changeDir( Dir.RIGHT )
                AR_LEFT -> changeDir( Dir.LEFT )
            }
        }

        // TBD separate game loop and render loop

        val delay = 1 / speedHz
        val back = rgb("#111111").opacify(0.2)

        val fieldMiddle = field.middle
        drawer.fontMap = defaultFontMap

        // update middle
        extend {
            drawer.image(bg, 0.0, 0.0, width.toDouble(), height.toDouble())
        }

        val fieldZero = mutableListOf(0.0, 0.0)

        fun SnakeCellType.asColor() = when( this ) {
            SnakeCellType.HEAD -> ColorRGBa.WHITE
            SnakeCellType.TAIL -> ColorRGBa.PINK
            SnakeCellType.EMPTY -> ColorRGBa.BLACK
            SnakeCellType.RABBIT -> ColorRGBa.GREEN
            SnakeCellType.STONE -> ColorRGBa.GRAY
        }

        // turn
        extend {
            if ( this.application.seconds - lastTurnSecs > delay ) {
                gameState.makeTurn(application.seconds - lastTurnSecs)
                lastTurnSecs = application.seconds
            }
        }

        val palette = colorSequence(
            0.0 to ColorRGBa(0.0, 1.0, 0.0, 0.2, Linearity.SRGB),
            1.0 to ColorRGBa(0.0, 0.0, 1.0, 0.2, Linearity.SRGB)
        )
        extend {
            val middle = mutableListOf( drawer.width / 2.0, drawer.height / 2.0)

            val cellWidth = calcCellWidth( drawer.width, drawer.height, field.width, field.height )
            //drawer.clear(ColorRGBa.TRANSPARENT)
            fun getCellCenter(pos: Coords, out: MutableList<Double>, cellWidth: Float) {
                // assert out.size == 2
                out[0] = middle[0] + ( pos[0] - fieldMiddle[0] + 0.5 ) * cellWidth.toDouble()
                out[1] = middle[1] - ( pos[1] - fieldMiddle[1] + 0.5 ) * cellWidth.toDouble()
            }
            drawer.circles {

                // by  axis
                (0 until field.width).forEach { x ->
                    (0 until field.height).forEach inner@ { y ->
                        val s = field.getState(x , y)
                        if ( s == SnakeCellType.EMPTY ) return@inner

                        fill = s.asColor()


/*fill = ColorRGBa(
                            0.0, //fill!!.r,
                        field.getCounter(x, y) / (gameState.lives + 0.0),
                            0.3, //fill!!.b
                        )*/



                        getCellCenter( coords(x, y), fieldZero, cellWidth )
                        circle( fieldZero[0], fieldZero[1], cellWidth / 2.0)
                    }
                }
            }

            drawer.fill = ColorRGBa.TRANSPARENT
            drawer.stroke = ColorRGBa.MAGENTA
            drawer.rectangle(
                middle[0] + ( 0 - fieldMiddle[0] ) * cellWidth,
                middle[1] + ( 0 - fieldMiddle[1] ) * cellWidth,
                field.width * cellWidth.toDouble(), field.height * cellWidth.toDouble()
            )
//            drawer.fill = ColorRGBa.MAGENTA
//            drawer.lineSegment(Vector2(10.0, 10.0), Vector2(20.0, 10.0))
//            drawer.stroke = ColorRGBa.RED
//            drawer.lineSegment(Vector2(10.0, 10.0), Vector2(10.0, 20.0))

            // score display
            val m = 0.05
            drawer.isolated {
                //drawer.translate(width / 2.0, height/2.0)
                drawer.scale(min(width, height) / 32.0)
                //drawer.text(x.toString(), 100.0, 100.0)
                fill = ColorRGBa(0.0, 0.9, 0.2, 0.75)
                stroke = ColorRGBa.TRANSPARENT
                drawer.rectangles {
                    gameState.lives.drawAsDigit(coords(0, 0), ::lineDigits).forEachIndexed { index, pos ->
                        // assert pos.size == 2
                        rectangle(pos[0].toDouble()+ m, pos[1].toDouble()+m, 1.0 - m, 1.0 - m )
                    }
                }

                //rectangle(0.0, 0.0, 1.0, 1.0 )
            }

            //debug
            drawer.isolated {
                stroke = ColorRGBa.TRANSPARENT
                drawer.scale(1.0, -1.0)
                drawer.translate(drawer.width / 2.0, -drawer.height / 2.0)
                drawer.scale(cellWidth.toDouble())

                drawer.translate(
                    (-field.middle[0] + 0.5),
                    (-field.middle[1] + 0.5)
                )


                drawer.circles {
                   field.forEach {


                       fill = palette.index(it.counter % 10 / 10.0)//ColorRGBa(0.0, (it.counter % 3).toDouble(), 0.0, 0.9)
                       //getCellCenter( coords(x, y), fieldZero, cellWidth )
                       circle( it.x.toDouble(), it.y.toDouble(), 0.5)
                   }

                }

            }

        }
    }
}

/*
rules {
    cellStates = listOf( EMPTY, STONE, HEAD, TAIL, RABBIT )
    torusSurfaceGrid(20, 20, cellStates) {

        state(EMPTY) { // parallel random  rabbit ?
         onUpdate { evt -> EMPTY }
        }

        state(STONE) { onUpdate { evt -> STONE } }
        state(HEAD) {
            //moore(
            neumann.forEach { c ->
                if (c ==
        }


    }
}
space . attributes for nodes
 */

fun Program.makeBackground() = drawImage(width, height, multisample = null) {
    drawCircles( this.width, this.height, 14000)
}

fun X(k: Int): Double {
    val pik = 2 * k * PI / 14000.0
    return cos( 5 * pik ) * (1 - 0.5 * (cos ( 8 * pik )).pow(2))
}

fun Y(k: Int): Double {
    val pik = 2 * k * PI / 14000.0
    return sin( 5 * pik ) * (1 - 0.5 * (cos ( 8 * pik )).pow(2))
}

fun R(k: Int): Double {
    return 1 / 200.0 + 0.1 * sin( 52 * k * PI / 14000.0 ).pow(4)
}


fun Drawer.drawCircles(w: Int, h: Int, maxi: Int = 14000, strokeWidthCb: (i: Int) -> Double = { i ->
    (sin(i * 4 * PI / maxi - 2 * PI ) + 1.1) * 1.5
}) {

    val palette = colorSequence( 0.0 to ColorRGBa.RED, 0.5 to ColorRGBa.GREEN, 1.0 to ColorRGBa.BLUE)
    //stroke = ColorRGBa(random(0.0, 1.0), random(0.0, 1.0), random(0.0, 1.0), 1.0)

    val rm = min(w, h)
    val hrm = rm / 2.0
    val hw = w / 2.0
    val hh = h / 2.0
    fill = ColorRGBa.TRANSPARENT

    circles {
// 14000
        (1 .. maxi ).forEach {
            strokeWeight = strokeWidthCb(it)
            stroke = palette.index( it.toDouble() / maxi ).saturated
            circle(X(it) * hrm  + hw, Y(it) * hrm + hh, R(it) * hrm)
        }
//        fill = ColorRGBa.RED
//        circle(10.0, 10.0, 10.0)
    }
//    stroke = ColorRGBa.CYAN
//    fill = ColorRGBa.TRANSPARENT
//    rectangle(0.0, 0.0, width.toDouble(), height.toDouble())
}