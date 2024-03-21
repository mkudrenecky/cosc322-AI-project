package ubc.cosc322;
import java.util.*;

public class Negamax {
    public int evaluation;
    public Action bestAction;
    private int nodeCount = 0;

    public Negamax(int evaluation, Action bestAction) {
        this.evaluation = evaluation;
        this.bestAction = bestAction;
    }

    public static Action findBestAction(Board board, int depth, int player, int heuristic, long startTime, long timeLimit) {
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        Negamax result = negamaxSearch(board, depth, alpha, beta, player, heuristic, startTime, timeLimit);

        Action bestAction = result.bestAction;

        return bestAction;
    }

    private static Negamax negamaxSearch(Board board, int depth, int alpha, int beta, int player, int heuristic, long startTime,long timeLimit) {
        List<Action> legalActions = ActionFactory.getActions(board, player);
        if (depth == 0 || legalActions.isEmpty()) {
            return new Negamax(evaluate(board, player, heuristic), null);
        }

        Action bestAction = null;
        int maxEval = Integer.MIN_VALUE;

        for (Action action : legalActions) {
            Board nextBoard = new Board(board, action);
            if (System.currentTimeMillis() - startTime >= timeLimit) {
                System.out.println("Time's up! Search interrupted.");
                return new Negamax(maxEval, bestAction); // Return the best action found so far
            }
            int eval = -negamaxSearch(nextBoard, depth - 1, -beta, -alpha, getOpponent(player), heuristic, startTime, timeLimit).evaluation;
            if (eval > maxEval) {
                maxEval = eval;
                bestAction = action;
            }
            alpha = Math.max(alpha, eval);
            if (beta <= alpha) {
                break;
            }
        }
        return new Negamax(maxEval, bestAction);
    }

    private static int evaluate(Board board, int player, int heuristic) {
        switch (heuristic) {
            case 1:
                return mobilityHeuristic(board, player);
            case 2:
                return territoryHeuristic(board, player);
            case 3:
                return improvedTerritoryHeuristic(board, player);
            case 4: 
                return combinedHeuristic(board, player);
            default:
                return 0; // Default evaluation if heuristic not specified
        }
    }

    private static int mobilityHeuristic(Board board, int player) {
        int mobilityScore = ActionFactory.getActions(board, player).size() - ActionFactory.getActions(board, getOpponent(player)).size();
        return mobilityScore;
    }

    private static int territoryHeuristic(Board board, int player) {
        ArrayList<Action> playerActions = ActionFactory.getActions(board, player);
        ArrayList<Action> opponentActions = ActionFactory.getActions(board, getOpponent(player));
        int playerTerritory = 0;
        for (int i = 0; i < board.getBoardSize(); i++) {
            for (int j = 0; j < board.getBoardSize(); j++) {
                
                int playerSquare = 0;
                int opponentSquare = 0;
                for (Action action : playerActions) {
                    if (action.getQueenMove().getEndCol() == i && action.getQueenMove().getEndRow() == j) {
                        playerSquare++;
                    }
                }
                for (Action action : opponentActions) {
                    if (action.getQueenMove().getEndCol() == i && action.getQueenMove().getEndRow() == j) {
                        opponentSquare++;
                    }
                }
                int squareOwner = playerSquare - opponentSquare;
                if (squareOwner < 0)
                    playerTerritory--;
                if (squareOwner > 0)
                    playerTerritory++;
            }
        }
        return playerTerritory;
    }

    private static int getOpponent(int player) {
        return player == 1 ? 2 : 1;
    }
    private static int improvedTerritoryHeuristic(Board board, int player) {
        int playerTerritoryScore = 0;
        ArrayList<Action> playerActions = ActionFactory.getActions(board, player);
        ArrayList<Action> opponentActions = ActionFactory.getActions(board, getOpponent(player));
    
        // Define a square importance matrix (example weights, adjust as needed)
        int[][] squareImportance = {
            {3, 3, 3, 3, 3, 3, 3, 3, 3, 3},
            {3, 2, 2, 2, 2, 2, 2, 2, 2, 3},
            {3, 2, 1, 1, 1, 1, 1, 1, 2, 3},
            {3, 2, 1, 0, 0, 0, 0, 1, 2, 3},
            {3, 2, 1, 0, 0, 0, 0, 1, 2, 3},
            {3, 2, 1, 0, 0, 0, 0, 1, 2, 3},
            {3, 2, 1, 1, 1, 1, 1, 1, 2, 3},
            {3, 2, 2, 2, 2, 2, 2, 2, 2, 3},
            {3, 3, 3, 3, 3, 3, 3, 3, 3, 3},
            {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}
        };
    
        // Iterate through each square on the board
        for (int i = 0; i < board.getBoardSize(); i++) {
            for (int j = 0; j < board.getBoardSize(); j++) {
                int playerSquareCount = 0;
                int opponentSquareCount = 0;
    
                // Assess square importance
                int importance = squareImportance[i][j];
    
                // Check accessibility and positional influence
                for (Action action : playerActions) {
                    if (canReachSquare(action, i, j, board)) {
                        playerSquareCount++;
                    }
                }
                for (Action action : opponentActions) {
                    if (canReachSquare(action, i, j, board)) {
                        opponentSquareCount++;
                    }
                }
    
                // Evaluate territorial control based on difference in accessibility
                int squareOwner = playerSquareCount - opponentSquareCount;
                playerTerritoryScore += squareOwner * importance;
            }
        }
        return playerTerritoryScore;
    }
    
    // Helper method to check if a queen can reach a square
    private static boolean canReachSquare(Action action, int targetRow, int targetCol, Board board) {
        int queenRow = action.getQueenMove().getEndRow();
        int queenCol = action.getQueenMove().getEndCol();
        int deltaRow = targetRow - queenRow;
        int deltaCol = targetCol - queenCol;
        int steps = Math.max(Math.abs(deltaRow), Math.abs(deltaCol));
    
        // Check if the square is reachable in any direction
        for (int i = 1; i <= steps; i++) {
            int checkRow = queenRow + deltaRow * i / steps;
            int checkCol = queenCol + deltaCol * i / steps;
            if (checkRow == targetRow && checkCol == targetCol && board.getPieceAt(checkRow, checkCol)==0) {
                return true;
            }
        }
        return false;
    }
    private static int combinedHeuristic(Board board, int player) {
        int mobilityScore = mobilityHeuristic(board, player);
        int territoryScore = improvedTerritoryHeuristic(board, player);
        
        // Weighted combination of mobility and territory scores
        int combinedScore = (int) (0.6 * mobilityScore + 0.4 * territoryScore);
        
        return combinedScore;
    }    
}
