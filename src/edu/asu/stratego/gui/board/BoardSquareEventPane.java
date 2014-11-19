/* Gorgon class. For the love of Zeus, don't look at it! */

package edu.asu.stratego.gui.board;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

import edu.asu.stratego.game.Game;
import edu.asu.stratego.game.GameStatus;
import edu.asu.stratego.game.MoveStatus;
import edu.asu.stratego.game.Piece;
import edu.asu.stratego.game.PieceColor;
import edu.asu.stratego.game.PieceType;
import edu.asu.stratego.game.board.ClientSquare;
import edu.asu.stratego.gui.board.setup.SetupPanel;
import edu.asu.stratego.gui.board.setup.SetupPieces;
import edu.asu.stratego.media.ImageConstants;
import edu.asu.stratego.util.HashTables;

/**
 * A single square within the BoardEventPane.
 */
public class BoardSquareEventPane extends GridPane {
    
    private ArrayList<Point> validMoves;
    private ImageView hover;
    
    /**
     * Creates a new instance of BoardSquareEventPane.
     */
    public BoardSquareEventPane() {
        hover = new ImageView(ImageConstants.HIGHLIGHT_NONE);
        
        // Event handlers for the square.
        hover.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, new OnHover());
        hover.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, new OffHover());
        hover.addEventHandler(MouseEvent.MOUSE_CLICKED, new SelectSquare());
        
        this.getChildren().add(hover);
    }
    
    /**
     * This event is triggered when the player's cursor is hovering over the 
     * BoardSquareEventPane. It changes the hover image to indicate to the user 
     * whether or not a square is valid.
     */
    private class OnHover implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent e) {
            ImageView hover = (ImageView) e.getSource();
            int row = GridPane.getRowIndex(hover);
            int col = GridPane.getColumnIndex(hover);
            
            // DAVID: Setting up
            if (Game.getStatus() == GameStatus.SETTING_UP) {
	            checkMove(row, col, hover);
	        }
            // DAVID: Waiting for opponent
            else if(Game.getStatus() == GameStatus.WAITING_OPP){ 
            	invalidMove(hover);
            }
            // DAVID: In progress
            else if(Game.getStatus() == GameStatus.IN_PROGRESS) {
            	if(Game.getMoveStatus() == MoveStatus.OPP_TURN)
            		invalidMove(hover);
            	else if(Game.getMoveStatus() == MoveStatus.NONE_SELECTED)
            		checkMove(row, col, hover);
            	else if(Game.getMoveStatus() == MoveStatus.START_SELECTED) {
            		// TODO function to only highlight squares the piece can move to
            	}
            }
        };
    }
    
    /**
     * This event is fired when the cursor leaves the square. It changes the 
     * hover image back to its default image: a blank image with a 1% fill.
     */
    private class OffHover implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent e) {
            ImageView hover = (ImageView) e.getSource();
            int row = GridPane.getRowIndex(hover);
            int col = GridPane.getColumnIndex(hover);
            
            if(Game.getStatus() == GameStatus.SETTING_UP)
            	hover.setImage(ImageConstants.HIGHLIGHT_NONE);
            else if(Game.getStatus() == GameStatus.WAITING_OPP) 
            	hover.setImage(ImageConstants.HIGHLIGHT_NONE);
            else if(Game.getStatus() == GameStatus.IN_PROGRESS) { 
            	if(Game.getMoveStatus() == MoveStatus.OPP_TURN) 
            		hover.setImage(ImageConstants.HIGHLIGHT_NONE);
            	else if(Game.getMoveStatus() == MoveStatus.NONE_SELECTED)
            		hover.setImage(ImageConstants.HIGHLIGHT_NONE);
            	else if(Game.getMoveStatus() == MoveStatus.START_SELECTED) {
            		// TODO function to only allow highlighting of squares piece can move to 
            	}
            }
        };
    }

    private void invalidMove(ImageView inImage) {
    	inImage.setImage(ImageConstants.HIGHLIGHT_INVALID);
    }
    private void validMove(ImageView inImage) {
    	inImage.setImage(ImageConstants.HIGHLIGHT_VALID);
    }
    private void checkMove(int row, int col, ImageView inImage) {
        if (isHoverValid(row, col))
            validMove(hover);
        else
            invalidMove(hover);
    }
    
    /**
     * This event is fired when the player clicks on the event square. 
     */
    private class SelectSquare implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent e) {
            // Square position.
        	ImageView source = (ImageView) e.getSource();
        	
        	int row = GridPane.getRowIndex((Node) source);
            int col = GridPane.getColumnIndex((Node) source);

            // The square and the piece at this position.
            BoardSquarePane squarePane = Game.getBoard()
                    .getSquare(row, col)
                    .getPiecePane();
            
            ClientSquare square = Game.getBoard().getSquare(row, col);
            Piece squarePiece = square.getPiece();
            
            // Player color.
            PieceColor playerColor = Game.getPlayer().getColor();
            
            /* Game Setup Handler */
            if (Game.getStatus() == GameStatus.SETTING_UP && isHoverValid(row, col)) {
                
                // Get the selected piece (piece type and count) from the SetupPanel.
                PieceType selectedPiece = SetupPieces.getSelectedPieceType();
                int selectedPieceCount = 0;
                if (selectedPiece != null)
                    selectedPieceCount = SetupPieces.getPieceCount(selectedPiece);
                
            	// If the square contains a piece...
                if (squarePiece != null) {
                    
                	// Remove the existing piece if it is the same piece on board as the 
                    // selected piece (in SetupPanel) or if no piece is selected (in SetupPanel).
                    if (squarePiece.getPieceType() == selectedPiece || selectedPiece == null) {
                        if (squarePiece.getPieceType() != null)
                            SetupPieces.incrementPieceCount(squarePiece.getPieceType());
                        squarePane.setPiece(null);
                        square.setPiece(null);
                    }
                    
                    // Replace the existing piece with the selected piece (in SetupPanel).
                    else if (squarePiece.getPieceType() != selectedPiece && selectedPieceCount > 0) {
                    	SetupPieces.decrementPieceCount(selectedPiece);
                    	SetupPieces.incrementPieceCount(squarePiece.getPieceType());
                        square.setPiece(new Piece(selectedPiece, playerColor, false));
                        squarePane.setPiece(HashTables.PIECE_MAP.get(square.getPiece().getPieceSpriteKey()));
                    }
                }
                
                // Otherwise, if the square does not contain a piece...
                else {
                	// Place a new piece on the square.
                    if (selectedPiece != null && selectedPieceCount > 0) {
                        square.setPiece(new Piece(selectedPiece, playerColor, false));
                        squarePane.setPiece(HashTables.PIECE_MAP.get(square.getPiece().getPieceSpriteKey()));
                        SetupPieces.decrementPieceCount(selectedPiece);
                    }
                }
            }
            else if(Game.getStatus() == GameStatus.IN_PROGRESS && Game.getTurn() == playerColor) {
            	// If it is the first piece being selected to move (start)
            	if(Game.getMoveStatus() == MoveStatus.NONE_SELECTED && isHoverValid(row, col)) {
            		Game.getMove().setStart(row, col);
            		Game.setMoveStatus(MoveStatus.START_SELECTED);
            		
            		validMoves = computeValidMoves(row, col);
            		displayValidMoves(row, col);
            	}
            	// If a start piece has already been selected, but user is changing start piece
            	else if(Game.getMoveStatus() == MoveStatus.START_SELECTED && !isNullPiece(row, col)) {
        			Piece highlightPiece = Game.getBoard().getSquare(row, col).getPiece();
        			if(highlightPiece.getPieceColor() == playerColor) {
                		Game.getMove().setStart(row, col);
                		
                		validMoves = computeValidMoves(row, col);
                		displayValidMoves(row, col);
        			}
            	}
            }
        }
    }
    
    private void displayValidMoves(int pieceRow, int pieceCol) {
        for (int row = 0; row < 10; ++row) {
            for (int col = 0; col < 10; ++col) {
                Game.getBoard().getSquare(row, col).getEventPane().getHover().setImage(ImageConstants.HIGHLIGHT_NONE);
                Game.getBoard().getSquare(row, col).getEventPane().getHover().setOpacity(1.0);
                Game.getBoard().getSquare(row, col).getPiecePane().getPiece().setEffect(new Glow(0.0));                      
            }
        }

        Game.getBoard().getSquare(pieceRow,pieceCol).getPiecePane().getPiece().setEffect(new Glow(0.75));                      
        Game.getBoard().getSquare(pieceRow,pieceCol).getEventPane().getHover().setImage(ImageConstants.HIGHLIGHT_WHITE);

        
        for (Point validMove : validMoves) {
            Game.getBoard().getSquare((int) validMove.getX(), (int) validMove.getY()).getEventPane().getHover().setImage(ImageConstants.HIGHLIGHT_VALID);
            Game.getBoard().getSquare((int) validMove.getX(), (int) validMove.getY()).getEventPane().getHover().setOpacity(0.5);
        }
    }
    
    private ArrayList<Point> computeValidMoves(int row, int col) {
    	int max = 1;
    	PieceType pieceType = Game.getBoard().getSquare(row, col).getPiece().getPieceType();
    	if(pieceType == PieceType.SCOUT)
    		max = 8;
    	
    	ArrayList<Point> validMoves = new ArrayList<Point>();
    	
    	if(pieceType != PieceType.BOMB && pieceType != PieceType.FLAG) {
	    	// Negative Row (UP)
	    	for(int i = -1; i >= -max; --i) {
	    		if(isInBounds(row+i,col) && (!isLake(row+i, col) || (!isNullPiece(row+i, col) && !isOpponentPiece(row+i, col)))) {
	    			if(isNullPiece(row+i, col) || isOpponentPiece(row+i, col)) {
	    				validMoves.add(new Point(row+i, col));
	    				
	    				if(!isNullPiece(row+i, col) && isOpponentPiece(row+i, col))
	    					break;
	    			}
	    			else
	    			    break;
	    		}
	    		else
	    			break;
	    	}
	    	// Positive Col (RIGHT)
	    	for(int i = 1; i <= max; ++i) {
	    		if(isInBounds(row,col+i) && (!isLake(row, col+i) || (!isNullPiece(row, col+i) && !isOpponentPiece(row, col+i)))) {
	    			if(isNullPiece(row, col+i) || isOpponentPiece(row, col+i)) {
	    				validMoves.add(new Point(row, col+i));
	    				
	    				if(!isNullPiece(row, col+i) && isOpponentPiece(row, col+i))
	    					break;
	    			}
	    			else
                        break;
	    		}
	    		else
	    			break;
	    	}
	    	// Positive Row (DOWN)
	    	for(int i = 1; i <= max; ++i) {
	    		if(isInBounds(row+i,col) && (!isLake(row+i, col) || (!isNullPiece(row+i, col) && !isOpponentPiece(row+i, col)))) {
	    			if(isNullPiece(row+i, col) || isOpponentPiece(row+i, col)) {
	    				validMoves.add(new Point(row+i, col));
	    				
	    				if(!isNullPiece(row+i, col) && isOpponentPiece(row+i, col))
	    					break;
	    			}
	    			else
                        break;
	    		}
	    		else
	    			break;
	    	}
	    	// Negative Col (LEFT)
	    	for(int i = -1; i >= -max; --i) {
	    		if(isInBounds(row,col+i) && (!isLake(row, col+i) || (!isNullPiece(row, col+i) && !isOpponentPiece(row, col+i)))) {
	    			if(isNullPiece(row, col+i) || isOpponentPiece(row, col+i)) {
	    				validMoves.add(new Point(row, col+i));
	    				
	    				if(!isNullPiece(row, col+i) && isOpponentPiece(row, col+i))
	    					break;
	    			}
	    			else
                        break;
	    		}
	    		else
	    			break;
	    	}
    	}
    	
    	for(int j = 0; j < validMoves.size(); j++) {
    		System.out.println(validMoves.get(j));
    	}
    	return validMoves;
    }
    
    private static boolean isLake(int row, int col) {
    	if (col == 2 || col == 3 || col == 6 || col == 7) {
            if (row == 4 || row == 5)
                return true;
        }
    	return false;
    }
    
    private static boolean isInBounds(int row, int col) {
    	if(row < 0 || row > 9)
    		return false;
    	if(col < 0 || col > 9)
    		return false;
    	
    	return true;
    }
    private static boolean isOpponentPiece(int row, int col) {
    	return Game.getBoard().getSquare(row, col).getPiece().getPieceColor() != Game.getPlayer().getColor();
    }
    private static boolean isNullPiece(int row, int col) {
    	return Game.getBoard().getSquare(row, col).getPiece() == null;
    }

    /**
     * During the Setup phase of the game, this method randomly places the 
     * pieces that have not yet been placed when the Setup Timer hits 0.
     */
    public static void randomSetup() {
        PieceColor playerColor = Game.getPlayer().getColor();
        
        for (int col = 0; col < 10; ++col) {
            for (int row = 6; row < 10; ++row) {
                BoardSquarePane squarePane = Game.getBoard().getSquare(row, col).getPiecePane();
                ClientSquare square = Game.getBoard().getSquare(row, col);
                Piece squarePiece = square.getPiece();
               
                ArrayList<PieceType> availTypes = 
                        new ArrayList<PieceType>(Arrays.asList(PieceType.values()));
               
                if(squarePiece == null) {
                        PieceType pieceType = null;
                       
                    while(pieceType == null) {
                        int randInt = (int) (Math.random() * availTypes.size());
                        if(SetupPieces.getPieceCount(availTypes.get(randInt)) > 0)
                            pieceType = availTypes.get(randInt);
                        else
                            availTypes.remove(randInt);
                    }
                   
                    square.setPiece(new Piece(pieceType, playerColor, false));
                    squarePane.setPiece(HashTables.PIECE_MAP.get(square.getPiece().getPieceSpriteKey()));
                    SetupPieces.decrementPieceCount(pieceType);
                }
            }
        }
        
        SetupPanel.finishSetup();
    }
    
    /**
     * Indicates whether or not a square is a valid square to click.
     * 
     * @param row row index of the square
     * @param col column index of the square
     * @return true if the square is valid, false otherwise
     */
    private boolean isHoverValid(int row, int col) {
    	PieceColor playerColor = Game.getPlayer().getColor();
    	
        /* Initially assumes that the square is valid. */
        
        // Lakes are always invalid.
        if (isLake(row, col))
                return false;
        
        // The game is setting up and the square is outside of the setup area.
        if (Game.getStatus() == GameStatus.SETTING_UP && row <= 5)
            return false;
        
        // The player has finished setting up and is waiting for the opponent.
        else if (Game.getStatus() == GameStatus.WAITING_OPP)
            return false;
        
        else if (Game.getStatus() == GameStatus.IN_PROGRESS) {
        	if(Game.getMoveStatus() == MoveStatus.OPP_TURN)
        		return false;
        		
        	if(Game.getMoveStatus() == MoveStatus.NONE_SELECTED) {
        		if(Game.getBoard().getSquare(row, col).getPiece() != null) {
        			Piece highlightPiece = Game.getBoard().getSquare(row, col).getPiece();
        			
        			if(highlightPiece.getPieceColor() != playerColor)
        				return false;
        		} else 
        			return false;

        	}
        }
        
        return true;
    }
    
    /**
     * @return the ImageView object that displays the hover image.
     */
    public ImageView getHover() {
        return hover;
    }
}