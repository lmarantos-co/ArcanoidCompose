package com.example.arcanoidcompose.Game

import androidx.compose.ui.graphics.Color

class GameState {

    data class block(var x: Float, var y : Float, var width : Float, var height : Float, var blockColor :Color, var numOfHits : Int)
    data class arcanoidBall(var x : Float, var y : Float, var ballRadius : Float, var ballSpeedX : Float,var ballSpeedY : Float)

    data class GameState(var allBlocks : MutableList<block>,
                         var playerRamp : MutableList<block>,
       var ball : arcanoidBall)

}