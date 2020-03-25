package model

package object boardConstants{
  val boardBonus: Map[(Int, Int), String] = Map((1, 5) -> constants.letterForTwo, (1, 13) -> constants.letterForTwo, (3, 8) -> constants.letterForTwo,
    (3, 10) -> constants.letterForTwo, (4, 9) -> constants.letterForTwo, (5, 1) ->constants.letterForTwo,
    (5, 17) ->constants.letterForTwo, (8, 3) -> constants.letterForTwo,(8, 8) ->constants.letterForTwo,
    (8, 10) ->constants.letterForTwo, (8, 15) ->constants.letterForTwo, (9, 4) ->constants.letterForTwo,
    (9, 14) ->constants.letterForTwo, (10, 3) ->constants.letterForTwo, (10, 8) ->constants.letterForTwo,
    (10, 10) ->constants.letterForTwo, (10, 15) ->constants.letterForTwo, (13, 1) ->constants.letterForTwo,
    (13, 17) ->constants.letterForTwo, (14, 9) ->constants.letterForTwo, (15, 8) ->constants.letterForTwo,
    (15, 10) ->constants.letterForTwo, (17, 5) ->constants.letterForTwo,(17,13) ->constants.letterForTwo,
    (2, 7) ->constants.letterForThree, (2, 11) ->constants.letterForThree, (7, 2) ->constants.letterForThree,
    (7, 7) ->constants.letterForThree, (7, 11) ->constants.letterForThree, (7, 16) ->constants.letterForThree,
    (11, 2) ->constants.letterForThree, (11, 7) ->constants.letterForThree, (11, 11) ->constants.letterForThree,
    (11, 16) ->constants.letterForThree, (16, 7) ->constants.letterForThree, (16, 11) ->constants.letterForThree,
    (1, 1) ->constants.wordForThree, (9, 1) ->constants.wordForThree, (17, 1) ->constants.wordForThree,
    (1, 9) ->constants.wordForThree, (17, 9) ->constants.wordForThree, (1, 17) ->constants.wordForThree,
    (9, 17) ->constants.wordForThree, (17, 17) ->constants.wordForThree,
    (2, 2) ->constants.wordForTwo, (3, 3) ->constants.wordForTwo, (4, 4) ->constants.wordForTwo,
    (5, 5) ->constants.wordForTwo, (6, 6) ->constants.wordForTwo, (12, 12) ->constants.wordForTwo,
    (13, 13) ->constants.wordForTwo, (14, 14) ->constants.wordForTwo, (15, 15) ->constants.wordForTwo,
    (16, 16) ->constants.wordForTwo, (2, 16) ->constants.wordForTwo, (3, 15) ->constants.wordForTwo,
    (4, 14) ->constants.wordForTwo, (5, 13) ->constants.wordForTwo, (6, 12) ->constants.wordForTwo,
    (12, 6) ->constants.wordForTwo, (13, 5) ->constants.wordForTwo, (14, 4) ->constants.wordForTwo,
    (15, 3) ->constants.wordForTwo, (16, 2) ->constants.wordForTwo)

}

// TODO implementazione della classe casella e del tabellone