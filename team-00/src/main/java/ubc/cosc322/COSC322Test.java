
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
	// private ActionFactory actionFactory;
	private boolean isBlack;

	/**
	 * The main method
	 * 
	 * @param args for name and passwd (current, any string would work)
	 */
	public static void main(String[] args) {
		GamePlayer player;
		testClass = new TestClass();
		// GamePlayer player2;
		// COSC322Test player = new COSC322Test(args[0], args[1]);
		player = new COSC322Test("sam", "123");
		// player2 = new COSC322Test("sam", "456");
		// player = new HumanPlayer();
		player.connect();
		// player2.connect();

		if (player.getGameGUI() == null) {
			player.Go();
			// player2.Go();
		} else {
			BaseGameGUI.sys_setup();
			java.awt.EventQueue.invokeLater(new Runnable() {
				public void run() {
					player.Go();
					// player2.Go();
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
				System.out.print("Lets go");
				// Black goes first
				isBlack = msgDetails.get(AmazonsGameMessage.PLAYER_BLACK).equals(getGameClient().getUserName());
				player = isBlack ? Board.BLACK_QUEEN : Board.WHITE_QUEEN;
				if (isBlack)
					System.out.print("Hello Black");
				makeMinMaxMove();
				// makeRandomMove();

				System.out.println(board.boardToString());
				break;
			case GameMessage.GAME_STATE_BOARD:
				// System.out.println("Message Details: " + msgDetails);
				// Board oldBoard = board;
				getGameGUI().setGameState((ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE));
				board = new Board((ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE));
				// System.out.println("Old board:");
				// System.out.println(oldBoard.boardToString());
				// System.out.println("New board:");
				// System.out.println(board.boardToString());
				// Action opponentAction = testClass.findMove(oldBoard, board);
				// System.out.println("Opponent move legal? " + testClass.checkIfMoveValid(opponentAction, getOpponent(player), oldBoard));
				// // makeMinMaxMove();
				// // System.out.println(board.boardToString());
				break;
			case GameMessage.GAME_ACTION_MOVE:
				// getGameGUI().updateGameState(msgDetails);
				Action opAction = new Action(msgDetails);
				// board.updateBoardState(opAction, board);
				QueenMove OPqueenMove = opAction.getQueenMove();
				ArrowShot OParrowShot = opAction.getArrowShot();
				// if (!isMoveLegal(OPqueenMove, OParrowShot)) {
				// 	System.out.println("Illegal move made: Queen move from (" + OPqueenMove.getStartRow() + ", "
				// 			+ OPqueenMove.getStartCol() + ") to (" + OPqueenMove.getEndRow() + ", "
				// 			+ OPqueenMove.getEndCol() + ") and arrow shot from (" + OParrowShot.getStartRow() + ", "
				// 			+ OParrowShot.getStartCol() + ") to (" + OParrowShot.getEndRow() + ", "
				// 			+ OParrowShot.getEndCol() + ")");
				// 	System.exit(1);
				// }
				getGameGUI().updateGameState(msgDetails);
				board.updateBoardState(opAction, board);
				if (isBlack) {
					// makeRandomMove();
					makeMinMaxMove();
				} else {
					// makeRandomMove();
					makeMinMaxMove();
				}

				// makeRandomMove();

				System.out.println(board.boardToString());
				if (ActionFactory.getActions(board, player == Board.BLACK_QUEEN ? Board.WHITE_QUEEN : Board.BLACK_QUEEN)
						.size() == 0) {
					System.out.println("We(" + player + ") won");
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
		// ArrayList<Action> actions = actionFactory.getActions(board, player);
		System.out.println("making move for black? " + isBlack);
		ArrayList<Action> actions = ActionFactory.getActions(board, isBlack ? Board.BLACK_QUEEN : Board.WHITE_QUEEN);

		System.out.println("WE made the actions");
		Action move = actions.get((int) (Math.random() * actions.size()));

		System.out.println("About to send move");
		getGameClient().sendMoveMessage(move.toServerResponse());
		getGameGUI().updateGameState(move.toServerResponse());

		board.updateBoardState(move, board);
	}

	private void makeMinMaxMove() {
		// will eventually use itertive deepening on a timer, tree will be smaller as
		// game progresses
		int depth = 2;
		int player = isBlack ? Board.BLACK_QUEEN : Board.WHITE_QUEEN;
		Action bestAction = MinMax.findBestAction(board, depth, player, 1);

		System.out.println("making min max move for black? " + isBlack);
		getGameClient().sendMoveMessage(bestAction.toServerResponse());
		getGameGUI().updateGameState(bestAction.toServerResponse());

		board.updateBoardState(bestAction, board);
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
