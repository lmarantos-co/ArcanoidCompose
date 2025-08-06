package com.example.arcanoidcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcanoidcompose.Game.GameState
import com.example.arcanoidcompose.ui.theme.ArcanoidCOmposeTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArcanoidGame()
        }
    }

    @Composable
    fun ArcanoidGame() {
        val screenWidth = remember { mutableStateOf(800f) }
        val screenHeight = remember { mutableStateOf(1200f) }

        val gameScore = remember { mutableStateOf(0) }
        val gameRunning = remember { mutableStateOf(true) }
        val gameStage = remember { mutableStateOf(1) }
        val newLineInterval = remember { mutableStateOf(50000) }
        val gameTimer = remember { mutableStateOf(0) }

        val numOfBlocksPerLine = 20
        val blockWidth = 40
        val currentArcBlocks = remember {
            mutableStateListOf<GameState.block>().apply {
                for (i in 0 until numOfBlocksPerLine) {
                    val x = i * (blockWidth)
                    val y = 40
                    add(GameState.block(x.toFloat(), y.toFloat(), blockWidth.toFloat(), blockWidth.toFloat(), Color.Blue , 1))
                }
            }
        }
        val theBall = remember {
            mutableStateOf(
                GameState.arcanoidBall(
                    x = 400f,
                    y = 600f,
                    ballRadius = 20f,
                    ballSpeedX = 10f,
                    ballSpeedY = 20f
                )
            )
        }

        val playerRamp = remember { mutableStateListOf<GameState.block>() }

        // Initialize ramp blocks
        LaunchedEffect(gameStage.value) {
            if (playerRamp.isEmpty()) {
                val startX = 320f
                repeat(5) { i ->
                    playerRamp.add(
                        GameState.block(
                            x = startX + i * blockWidth,
                            y = 1200f,
                            width = blockWidth.toFloat(),
                            height = blockWidth.toFloat(),
                            Color.Blue,
                            1
                        )
                    )
                }
            }
            theBall.value = theBall.value.copy(
                x = 400f,
                y = 200f,
                ballRadius = 20f,
                ballSpeedX = 10f,
                ballSpeedY = 20f
            )
        }

        // Ramp movement state
        val rampIsMovingLeft = remember { mutableStateOf(false) }
        val rampIsMovingRight = remember { mutableStateOf(false) }
        val rampIsStationary = remember { mutableStateOf(true) }

        val gameState = remember {
            mutableStateOf(
                GameState.GameState(
                    allBlocks = currentArcBlocks,
                    ball = theBall.value,
                    playerRamp = playerRamp
                )
            )
        }

        var ballCollisionOnFrame by remember { mutableStateOf(false) }

//        addExtraLineInArcanoidBlocks(gameState.value , 20 , blockWidth)

        LaunchedEffect(gameStage.value) {
            while (true) {
                withFrameMillis {
                    ballCollisionOnFrame = false

                    // Move ramp if button pressed
                    if (rampIsMovingLeft.value) {
                        moveRamp(-20f, playerRamp)
                    } else if (rampIsMovingRight.value) {
                        moveRamp(20f, playerRamp)
                    }

                    val allBlocks = gameState.value.allBlocks ?: return@withFrameMillis
                    val rampBlocks = gameState.value.playerRamp ?: return@withFrameMillis

                    // Add new line of blocks
                    if ((gameTimer.value >= newLineInterval.value) && (gameTimer.value % newLineInterval.value == 0)) {
                        addExtraLineInArcanoidBlocks(
                            gameState.value,
                            numOfBlocksPerLine,
                            blockWidth,
                            gameStage = gameStage.value
                        )
                    }

                    // Ball collision with ramp blocks
                    val collidedBlocks =
                        rampBlocks.filter { block -> isBallCollidingBlock(theBall.value, block) }

                    if (collidedBlocks.isNotEmpty()) {
                        val hitBlock = collidedBlocks.first()
                        ballCollisionOnFrame = true

                        // Reverse vertical speed
                        theBall.value = theBall.value.copy(ballSpeedY = -theBall.value.ballSpeedY)

                        // Move the ball out of the block (just above the block)
                        val newY = hitBlock.y - theBall.value.ballRadius * 2 - 1f
                        theBall.value = theBall.value.copy(y = newY)

                        // Optionally, tweak ballSpeedX based on ramp movement or hit position
                        // Calculate the horizontal hit position relative to the block center
                        val blockCenterX = hitBlock.x + hitBlock.width / 2f
                        val hitPositionOffset = (theBall.value.x - blockCenterX) / (hitBlock.width / 2f) // Range roughly -1 to 1

// Adjust horizontal speed based on hit position and ramp movement
                        val rampMovementEffect = when {
                            rampIsMovingLeft.value -> -1f
                            rampIsMovingRight.value -> 1f
                            else -> 0f
                        }

// Base horizontal speed adjustment — you can tweak the multiplier (e.g., 3f) to control sensitivity
                        val horizontalSpeedChange = hitPositionOffset * 3f + rampMovementEffect * 2f

// Update ballSpeedX, clamping it to a reasonable range to avoid it going too fast
                        val newBallSpeedX = (theBall.value.ballSpeedX + horizontalSpeedChange).coerceIn(-10f, 10f)

                        theBall.value = theBall.value.copy(ballSpeedX = newBallSpeedX)

                    }

                    // Ball collision with walls
                    if (theBall.value.x + theBall.value.ballSpeedX < 0 || theBall.value.x + theBall.value.ballSpeedX > screenWidth.value) {
                        theBall.value = theBall.value.copy(ballSpeedX = -theBall.value.ballSpeedX)
                        ballCollisionOnFrame = true
                    }
                    if (theBall.value.y + theBall.value.ballSpeedY < 0) {
                        theBall.value = theBall.value.copy(ballSpeedY = -theBall.value.ballSpeedY)
                        ballCollisionOnFrame = true
                    }

                    // Ball collision with blocks
                    val blocksHit = allBlocks.filter { block ->
                        block.x < theBall.value.x + theBall.value.ballRadius &&
                                block.x + block.width > theBall.value.x - theBall.value.ballRadius &&
                                block.y < theBall.value.y + theBall.value.ballRadius &&
                                block.y + block.height > theBall.value.y - theBall.value.ballRadius
                    }

                    if (blocksHit.isNotEmpty()) {
                        val hitBlock = blocksHit.first()

                        if (theBall.value.ballSpeedY < 0) {
                            // Ball is moving upwards and hit the block from below
                            // Place ball just below the block to prevent it from getting stuck inside
                            val newY = hitBlock.y + hitBlock.height + theBall.value.ballRadius + 1f
                            theBall.value = theBall.value.copy(y = newY)
                        } else {
                            // Ball is moving downwards and hit the block from above
                            // Place ball just above the block
                            val newY = hitBlock.y - theBall.value.ballRadius * 2 - 1f
                            theBall.value = theBall.value.copy(y = newY)
                        }

                        // Reverse vertical speed to bounce the ball
                        theBall.value = theBall.value.copy(ballSpeedY = -theBall.value.ballSpeedY)

                        hitBlock.numOfHits = hitBlock.numOfHits - 1
                        if (hitBlock.numOfHits == 0)
                        {
                            // Remove the hit block
                            gameState.value.allBlocks = gameState.value.allBlocks.filterNot { it == hitBlock }.toMutableList()
                            gameScore.value = gameScore.value + 100
                            ballCollisionOnFrame = true
                        }
                    }

                    if (allBlocks.isEmpty())
                    {
                        //move to next stage
                        gameStage.value = gameStage.value + 1
                        allBlocks.add(GameState.block(0f, 0f ,0f, 0f, Color.Blue, 0))
                        newLineInterval.value = newLineInterval.value - 5000
                    }

                    // Update ball position
                    if (ballCollisionOnFrame) {
                        gameState.value = gameState.value.copy(ball = theBall.value)
                    } else {
                        val newX = theBall.value.x + theBall.value.ballSpeedX
                        val newY = theBall.value.y + theBall.value.ballSpeedY
                        theBall.value = theBall.value.copy(x = newX, y = newY)
                        gameState.value = gameState.value.copy(ball = theBall.value)
                    }

                    // Check if ball falls below screen
                    if (theBall.value.y > screenHeight.value) {
                        gameRunning.value = false
                    }
                }
                delay(16L) // ~60 FPS
                gameTimer.value += 50
            }
        }

        // UI Layout
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {

            // Game Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Draw blocks
                gameState.value.allBlocks.forEach { block ->
// Draw black border (a bit bigger than the block)
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(block.x, block.y),
                        size = Size(block.width, block.height)
                    )

// Draw the block itself
                    drawRect(
                        color = block.blockColor,
                        topLeft = Offset(block.x, block.y),
                        size = Size(block.width - 2f, block.height - 2f)
                    )                }

                // Draw ramp
                gameState.value.playerRamp.forEach { block ->
                    drawRect(Color.Green, Offset(block.x, block.y), Size(block.width, block.height))
                }

                // Draw ball
                drawCircle(
                    Color.Red,
                    radius = gameState.value.ball.ballRadius,
                    center = Offset(gameState.value.ball.x, gameState.value.ball.y)
                )
            }

            // On-screen control buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RampControlButton(
                    text = "⬅️",
                    onPress = {
                        rampIsMovingLeft.value = true
                        rampIsMovingRight.value = false
                        rampIsStationary.value = false
                    },
                    onRelease = {
                        rampIsMovingLeft.value = false
                        rampIsStationary.value = true
                    }
                )
                RampControlButton(
                    text = "➡️",
                    onPress = {
                        rampIsMovingRight.value = true
                        rampIsMovingLeft.value = false
                        rampIsStationary.value = false
                    },
                    onRelease = {
                        rampIsMovingRight.value = false
                        rampIsStationary.value = true
                    }
                )
            }
            Spacer(modifier = Modifier.height(100.dp))
            Row(horizontalArrangement = Arrangement.Center)
            {
                Text(text = "Stage : ${gameStage.value}")
                Spacer(modifier = Modifier.width(50.dp))
                Text(text = "Score : ${gameScore.value}")
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun RampControlButton(
        onPress: () -> Unit,
        onRelease: () -> Unit,
        text: String
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                .pointerInteropFilter { motionEvent ->
                    when (motionEvent.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            onPress()
                            true
                        }
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL -> {
                            onRelease()
                            true
                        }
                        else -> false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontSize = 24.sp)
        }
    }


    // Helper function to move the ramp blocks, keeping them inside screen bounds
    fun moveRamp(deltaX: Float, playerRamp: MutableList<GameState.block>) {
        val minX = 0f
        val maxX = 800f // screen width
        val canMoveLeft = playerRamp.minOf { it.x } + deltaX >= minX
        val canMoveRight = playerRamp.maxOf { it.x + it.width } + deltaX <= maxX

        if ((deltaX < 0 && canMoveLeft) || (deltaX > 0 && canMoveRight)) {
            playerRamp.forEachIndexed { index, block ->
                playerRamp[index] = block.copy(x = block.x + deltaX)
            }
        }
    }


    fun isBallCollidingBlock(ball: GameState.arcanoidBall, block: GameState.block): Boolean {
        val ballCenterX = ball.x
        val ballCenterY = ball.y
        val radius = ball.ballRadius

        val blockLeft = block.x
        val blockRight = block.x + block.width
        val blockTop = block.y
        val blockBottom = block.y + block.height

        // Find closest point on block to ball center
        val closestX = ballCenterX.coerceIn(blockLeft, blockRight)
        val closestY = ballCenterY.coerceIn(blockTop, blockBottom)

        // Calculate distance to closest point
        val dx = ballCenterX - closestX
        val dy = ballCenterY - closestY

        return dx * dx + dy * dy < radius * radius
    }

    private fun blockColor(random : Int) : Color
    {
        var random = Random.nextInt(random)
        var color = Color.Blue
        when (random)
        {
            0 ->
            {
                color = Color.Blue
            }
            1 ->
            {
                color = Color.Yellow
            }
            2 ->
            {
                color = Color.Magenta
            }
            3->
            {
                color = Color.Red
            }
        }
        return color
    }

    private fun addExtraLineInArcanoidBlocks(gameState: GameState.GameState , numOfBlocksPerLine : Int, blockSize : Int, gameStage : Int)
    {
        // Move all blocks down
        for (i in gameState.allBlocks.indices) {
            gameState.allBlocks[i] =
                gameState.allBlocks[i].copy(y = gameState.allBlocks[i].y + blockSize)
        }

        // Add new blocks on top
        for (i in 0 until numOfBlocksPerLine) {
            var newColor = blockColor(gameStage)
            var numOfHits = 1
            when (newColor)
            {
                Color.Blue ->
                {
                    numOfHits =1
                }
                Color.Yellow ->
                {
                    numOfHits = 2
                }
                Color.Magenta ->
                {
                    numOfHits = 3
                }
                Color.Red ->
                {
                    numOfHits = 4
                }
            }
            gameState.allBlocks.add(
                GameState.block(
                    x = i.toFloat() * (blockSize.toFloat()),
                    y = 0f,
                    width = blockSize.toFloat(),
                    height = blockSize.toFloat(),
                    blockColor = blockColor(gameStage),
                    numOfHits = numOfHits
                )
            )
        }
    }
}
