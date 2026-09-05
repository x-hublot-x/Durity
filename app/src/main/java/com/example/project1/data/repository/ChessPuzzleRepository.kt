package com.example.project1.data.repository

import com.example.project1.data.model.ChessPuzzle

object ChessPuzzleRepository {
    private val puzzles = listOf(
        // 1. Детский мат (Ферзь бьет f7)
        ChessPuzzle(
            id = 1,
            fen = "r1bqkb1r/pppp1ppp/2n5/4p3/2B1P3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Qxf7"
        ),
        // 2. Спертый мат конем (Конь на f7)
        ChessPuzzle(
            id = 2,
            fen = "6rk/6pp/7N/8/8/8/8/4K3 w - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Nf7"
        ),
        // 3. Линейный мат по последней горизонтали (Ладья e8)
        ChessPuzzle(
            id = 3,
            fen = "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Re8"
        ),
        // 4. Арабский мат (Ладья h7 при поддержке Коня)
        ChessPuzzle(
            id = 4,
            fen = "7k/R7/5N2/8/8/8/8/7K w - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Rh7"
        ),
        // 5. Мат Ферзем под защитой пешки (Ферзь g7)
        ChessPuzzle(
            id = 5,
            fen = "6k1/5p1p/5P2/8/8/8/8/6QK w - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Qg7"
        ),
        // 6. Эполетный мат Ферзем (Ферзь e6)
        ChessPuzzle(
            id = 6,
            fen = "3rk2/8/8/8/8/8/8/8/Q5K1 w - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Qa6"
        ),
        // 7. Мат Ферзем и Слоном на h7 (Ферзьxh7)
        ChessPuzzle(
            id = 7,
            fen = "r1bq1rk1/ppp2ppp/2n5/3p3Q/3P4/3B4/PPP2PPP/R1B1K2R w KQ - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Qxh7"
        ),
        // 8. Мат Ладьей по первой горизонтали (Ладья a8)
        ChessPuzzle(
            id = 8,
            fen = "7k/8/7K/8/8/8/8/R7 w - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Ra8"
        ),
        // 9. Мат Ферзем на d7 (Ферзь d7)
        ChessPuzzle(
            id = 9,
            fen = "4kr2/1N6/8/8/7B/7Q/8/5RK1 b KQkq - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Qe6"
        ),
        // 10. Превращение пешки в ферзя с матом (Пешка h8=Q)
        ChessPuzzle(
            id = 10,
            fen = "k7/7P/K7/8/8/8/8/8 w - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "h8=Q"
        ),
        ChessPuzzle(
            id = 11,
            fen = "1n1r3k/r5p1/p4pP1/1ppB4/P7/7b/1P1K4/7R b KQkq - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Rh3"
        ),
        ChessPuzzle(
            id = 12,
            fen = "5R1R/6k1/1r1P2p1/6PP/pn1b3n/8/Pp6/1K6 w - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "h6"
        ),
        ChessPuzzle(
            id = 13,
            fen = "5n1r/p4Qpk/R7/2b5/1p2r3/1P4qP/6P1/2B2R1K b - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Rh6"
        ),
        ChessPuzzle(
            id = 14,
            fen = "N2kr3/p5pp/1p2Q2n/8/8/8/PP3PPP/R4RK1 b - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Rd1"
        ),
        ChessPuzzle(
            id = 15,
            fen = "8/8/4Q3/p7/2N3N1/P6B/1K6/6k1 b - - 0 1",
            instruction = "Ход белых. Поставьте мат в 1 ход!",
            correctMove = "Qe1"
        )
    )

    fun getRandomPuzzle(): ChessPuzzle {
        return puzzles.random()
    }
}
