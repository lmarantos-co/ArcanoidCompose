package com.example.arcanoidcompose.Game

class GameState {

    data class block(var x: Float, var y : Float, var width : Float, var height : Float)
    data class arcanoidBall(var x : Float, var y : Float, var ballRadius : Float, var ballSpeedX : Float,var ballSpeedY : Float)

    data class GameState(var allBlocks : MutableList<block>,
                         var playerRamp : MutableList<block>,
       var ball : arcanoidBall)

}