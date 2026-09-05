package com.example.project1.ui.screens.captchas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.project1.data.model.ChessPuzzle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun fenToBoard(fen: String): List<List<Char>> {
    val boardPart = fen.substringBefore(' ')
    return boardPart.split("/").map { row ->
        val cells = mutableListOf<Char>()
        for (c in row) {
            if (c.isDigit()) {
                repeat(c.digitToInt()) { cells.add(' ') }
            } else {
                cells.add(c)
            }
        }
        cells
    }
}

private fun pieceSymbol(c: Char): String = when (c) {
    'K' -> "♔"; 'Q' -> "♕"; 'R' -> "♖"; 'B' -> "♗"; 'N' -> "♘"; 'P' -> "♙"
    'k' -> "♚"; 'q' -> "♛"; 'r' -> "♜"; 'b' -> "♝"; 'n' -> "♞"; 'p' -> "♟"
    else -> ""
}

private fun fileToCol(file: Char): Int = file - 'a'
private fun rankToRow(rank: Char): Int = 8 - rank.digitToInt()

private fun isValidMove(
    toRow: Int,
    toCol: Int,
    movingPiece: Char,
    correctMove: String
): Boolean {
    val cleanMove = correctMove.replace("#", "").replace("+", "")
    val targetPart = cleanMove.substringBefore("=").takeLast(2)

    if (targetPart.length < 2) return false

    val targetCol = fileToCol(targetPart[0])
    val targetRow = rankToRow(targetPart[1])

    if (toRow != targetRow || toCol != targetCol) return false

    val expectedPieceType = when {
        cleanMove[0].isUpperCase() -> cleanMove[0]
        else -> 'P'
    }

    return movingPiece.uppercaseChar() == expectedPieceType
}

@Composable
fun ChessCaptchaDialog(
    puzzle: ChessPuzzle,
    onSolved: () -> Unit,
    onDismiss: () -> Unit
) {
    var board by remember(puzzle) { mutableStateOf(fenToBoard(puzzle.fen)) }
    var selectedPosition by remember(puzzle) { mutableStateOf<Pair<Int, Int>?>(null) }
    var errorMessage by remember(puzzle) { mutableStateOf<String?>(null) }
    var isSolvedAnimation by remember(puzzle) { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    var animatingPiece by remember { mutableStateOf<Char?>(null) }
    val animPosX = remember { Animatable(0f) }
    val animPosY = remember { Animatable(0f) }

    var boardSizePx by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 360.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Проверка безопасности",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = puzzle.instruction,
                        color = Color(0xFFB0B0C3),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(3.dp, Color(0xFF4285F4), RoundedCornerShape(12.dp))
                            .onGloballyPositioned { coordinates ->
                                boardSizePx = coordinates.size
                            }
                    ) {
                        val squareSizePx = boardSizePx.width / 8f

                        Column(modifier = Modifier.fillMaxSize()) {
                            for (row in 0..7) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    for (col in 0..7) {
                                        val isLight = (row + col) % 2 == 0
                                        val squareColor = if (isLight) Color(0xFFEEEED2) else Color(0xFF769656)
                                        val isSelected = selectedPosition == Pair(row, col)
                                        val piece = board[row][col]

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(
                                                    when {
                                                        isSelected -> Color(0xFFBBCB41).copy(alpha = 0.8f)
                                                        else -> squareColor
                                                    }
                                                )
                                                .clickable {
                                                    if (isSolvedAnimation || animatingPiece != null) return@clickable

                                                    val sel = selectedPosition
                                                    if (sel == null) {
                                                        if (piece != ' ' && piece.isUpperCase()) {
                                                            selectedPosition = Pair(row, col)
                                                            errorMessage = null
                                                        }
                                                    } else {
                                                        val (fromR, fromC) = sel

                                                        if (fromR == row && fromC == col) {
                                                            selectedPosition = null
                                                            return@clickable
                                                        }

                                                        val movingPiece = board[fromR][fromC]
                                                        val isCorrect = isValidMove(
                                                            row, col, movingPiece, puzzle.correctMove
                                                        )

                                                        if (isCorrect) {
                                                            val startX = fromC * squareSizePx
                                                            val startY = fromR * squareSizePx
                                                            val targetX = col * squareSizePx
                                                            val targetY = row * squareSizePx

                                                            val mutableBoard = board.map { it.toMutableList() }.toMutableList()
                                                            mutableBoard[fromR][fromC] = ' '
                                                            board = mutableBoard

                                                            animatingPiece = movingPiece
                                                            selectedPosition = null

                                                            coroutineScope.launch {
                                                                animPosX.snapTo(startX)
                                                                animPosY.snapTo(startY)

                                                                launch { animPosX.animateTo(targetX, animationSpec = tween(280)) }
                                                                launch { animPosY.animateTo(targetY, animationSpec = tween(280)) }.join()

                                                                val finalBoard = board.map { it.toMutableList() }.toMutableList()
                                                                // Превращение пешки на 8 горизонтали
                                                                val finalPiece = if (movingPiece == 'P' && row == 0) 'Q' else movingPiece
                                                                finalBoard[row][col] = finalPiece
                                                                board = finalBoard

                                                                animatingPiece = null
                                                                isSolvedAnimation = true
                                                            }
                                                        } else {
                                                            errorMessage = "Неверный ход. Попробуйте снова."
                                                            selectedPosition = null
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (piece != ' ') {
                                                PieceWidget(piece = piece)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Слой для анимации фигуры от TopStart всей доски
                        if (animatingPiece != null && squareSizePx > 0f) {
                            val pieceSizeDp = with(density) { squareSizePx.toDp() }
                            Box(
                                modifier = Modifier
                                    .size(pieceSizeDp)
                                    .offset {
                                        IntOffset(
                                            animPosX.value.roundToInt(),
                                            animPosY.value.roundToInt()
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                PieceWidget(piece = animatingPiece!!)
                            }
                        }

                        if (isSolvedAnimation) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color(0xFF4CAF50), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            LaunchedEffect(Unit) {
                                delay(800)
                                onSolved()
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Отмена", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieceWidget(piece: Char) {
    val symbol = pieceSymbol(piece)
    val isWhitePiece = piece.isUpperCase()

    if (isWhitePiece) {
        Box {
            Text(
                text = symbol,
                fontSize = 28.sp,
                color = Color.Black,
                style = LocalTextStyle.current.copy(
                    drawStyle = Stroke(width = 3f)
                )
            )
            Text(
                text = symbol,
                fontSize = 28.sp,
                color = Color.White
            )
        }
    } else {
        Text(
            text = symbol,
            fontSize = 28.sp,
            color = Color.Black
        )
    }
}
