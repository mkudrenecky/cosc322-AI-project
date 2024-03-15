
package ubc.cosc322;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.plaf.nimbus.State;

import sfs2x.client.entities.Room;
import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;
import ygraph.ai.smartfox.games.amazons.HumanPlayer;

/**
 * An example illustrating how to implement a GamePlayer
 * 
 * @author Yong Gao (yong.gao@ubc.ca)
 *         Jan 5, 2021
 *
 */
public class COSC322Test extends GamePlayer {

	private GameClient gameClient = null;
	private BaseGameGUI gamegui = null;

	private String userName = null;
	private String passwd = null;
	private Board board;
	private int player;
	private Action action;
	private static TestClass testClass;
	private boolean isBlack;
   

	/**
	 * The main method
	 * 
	 * @param args for name and passwd (current, any string would work)
	 */
	public static void main(String[] args) {

		GamePlayer player;
        GamePlayer player2;
		testClass = new TestClass();
		
		// COSC322Test player = new COSC322Test(args[0], args[1]);

        // creates two players to have them play eachother, launches a GUI for each player
		player = new COSC322Test("aaa", "123");
		player2 = new COSC322Test("sam", "456");

        // code for human player
		// player = new HumanPlayer();

		player.connect();
		player2.connect();

		if (player.getGameGUI() == null) {
			player.Go();
			player2.Go();
		} else {
			BaseGameGUI.sys_setup();
			java.awt.EventQueue.invokeLater(new Runnable() {
				public void run() {
					player.Go();
					player2.Go();
				}
			});
		}
	}

	/**
	 * Any name and passwd
	 * 
	 * @param userName
	 * @param passwd
	 */
	public COSC322Test(String userName, String passwd) {
		this.userName = userName;
		this.passwd = passwd;

		// To make a GUI-based player, create an instance of BaseGameGUI
		// and implement the method getGameGUI() accordingly
		this.gamegui = new BaseGameGUI(this);
	}

	@Override
	public void onLogin() {
		if (gameClient != null && gameClient.getUserName() != null) {
			userName = gameClient.getUserName();
			if (gamegui != null) {
				gamegui.setRoomInformation(gameClient.getRoomList());
			}
		} else {
			System.err.println("Error: Game client or user is not initialized.");
		}
	}

	@Override
	public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {

		switch (messageType) {
			case GameMessage.GAME_ACTION_START:
				
				// Black goes first, check if we are black
				isBlack = msgDetails.get(AmazonsGameMessage.PLAYER_BLACK).equals(getGameClient().getUserName());

                // set our player value
				player = isBlack ? Board.BLACK_QUEEN : Board.WHITE_QUEEN;

                // if we are black, we make the first move
				if (isBlack){
					System.out.print("Hello Black");
				    makeMinMaxMove();

                    // display the board after opening move
                    System.out.println("BOARD AFTER OPENING BLACK MOVE:");
				    System.out.println(board.boardToString());
                }

				System.out.println(board.boardToString());
				break;
			case GameMessage.GAME_STATE_BOARD:
                // this is the opening state of the game of the game
				System.out.println("Message Details: " + msgDetails);
                
                // set the gui to the starting state
				getGameGUI().setGameState((ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE));

                // instantiate our board to the starting state from the server
				board = new Board((ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE));

                // display the initial board for sanity
                System.out.println("Initial Board:");
				System.out.println(board.boardToString());
				
				break;
			case GameMessage.GAME_ACTION_MOVE:
				// add in some sanity checks to visualize the board, there are a lot of sanity checks here
                System.out.println("old board:");
                System.out.println(board.boardToString());

                // create a copy of the current board and display
                Board oldBoard = new Board(board);
                System.out.println("old board v2:");
                System.out.println(oldBoard.boardToString());

                // get the opponent action from the server and update the board
                Action opAction = new Action(msgDetails);
                board.updateBoardState(opAction);

                // display the board reflecting opponent move
                System.out.println("new board:");
                System.out.println(board.boardToString());

                // test the opponent move against the validator
                Action opponentAction = testClass.findMove(oldBoard, board);
				System.out.println("Opponent move legal? " + testClass.checkIfMoveValid(opponentAction, getOpponent(player), oldBoard));

                // shut down the game if opponent makes an illegal move
				if (!testClass.checkIfMoveValid(opponentAction, getOpponent(player), oldBoard)){
                System.out.println("Opponent has made an illegal move!");
                    System.exit(0);
                }
		
                // update the gui if all checks pass
				getGameGUI().updateGameState(msgDetails);

                // make our move
                // currently set up to play against white making random moves
				if (isBlack) {
					// makeRandomMove();
					makeMinMaxMove();
				} else {
					makeRandomMove();
					// makeMinMaxMove();
				}

				// Additional visualization to ensure board is as expected
                System.out.println("BOARD AFTER MOVE:");
				System.out.println(board.boardToString());

                // end game condition checking
				if (ActionFactory.getActions(board, player == Board.BLACK_QUEEN ? Board.WHITE_QUEEN : Board.BLACK_QUEEN)
						.size() == 0) {
					System.out.println("We(" + player + ") won");
                    //System.exit(0);
				}
				break;
			default:
				assert (false);
		}
		return true;

	}

	private int getOpponent(int player){
		return  (player == 1 ? 2 : 1);
	}

	private void makeRandomMove() {
		System.out.println("making move for black? " + isBlack);
		ArrayList<Action> actions = ActionFactory.getActions(board, isBlack ? Board.BLACK_QUEEN : Board.WHITE_QUEEN);

		System.out.println("WE made the actions");
		Action move = actions.get((int) (Math.random() * actions.size()));
        if (actions.size() == 0){
            System.out.println("We("+player+") are out of moves, we lost!! :( :( :( ");
            System.exit(0);
        }

		System.out.println("About to send random move");
		getGameClient().sendMoveMessage(move.toServerResponse());
		getGameGUI().updateGameState(move.toServerResponse());

		board.updateBoardState(move);
	}

	private void makeMinMaxMove() {
		// will eventually use iterative deepening on a timer, tree will be smaller as
		// game progresses
		int depth = 1;
		int player = isBlack ? Board.BLACK_QUEEN : Board.WHITE_QUEEN;
		Action bestAction = MinMax.findBestAction(board, depth, player, 1);
        if (bestAction == null){
            System.out.println("We("+player+") are out of moves, we lost!! :( :( :( ");
            System.exit(0);
        }

		System.out.println("making min max move for black? " + isBlack);
		getGameClient().sendMoveMessage(bestAction.toServerResponse());
		getGameGUI().updateGameState(bestAction.toServerResponse());

		board.updateBoardState(bestAction);
	}

	@Override
	public String userName() {
		return userName;
	}

	@Override
	public GameClient getGameClient() {
		// TODO Auto-generated method stub
		return this.gameClient;
	}

	@Override
	public BaseGameGUI getGameGUI() {
		// TODO Auto-generated method stub
		return this.gamegui;
	}

	@Override
	public void connect() {
		// TODO Auto-generated method stub
		gameClient = new GameClient(userName, passwd, this);
	}

}// end of class
