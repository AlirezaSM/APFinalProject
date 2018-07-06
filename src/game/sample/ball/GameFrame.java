/*** In The Name of Allah ***/
package game.sample.ball;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

/**
 * The window on which the rendering is performed.
 * This example uses the modern BufferStrategy approach for double-buffering, 
 * actually it performs triple-buffering!
 * For more information on BufferStrategy check out:
 *    http://docs.oracle.com/javase/tutorial/extra/fullscreen/bufferstrategy.html
 *    http://docs.oracle.com/javase/8/docs/api/java/awt/image/BufferStrategy.html
 * 
 * @author Seyed Mohammad Ghaffarian
 */
public class GameFrame extends JFrame {
	
	public static final int GAME_HEIGHT = 720;                  // 720p game resolution
	public static final int GAME_WIDTH = 16 * GAME_HEIGHT / 9;  // wide aspect ratio

	//uncomment all /*...*/ in the class for using Tank icon instead of a simple circle
	private BufferedImage tankBody;
	private BufferedImage tankGun;
	private BufferedImage opponentTank;
	private BufferedImage opponentTankGun;

    ConstantEnemy ab = new ConstantEnemy();

	private long lastRender;
	private ArrayList<Float> fpsHistory;

	private BufferStrategy bufferStrategy;
	Map a = new Map();
	double tankGunAngle;
	ArrayList<Bullet> bullets = new ArrayList<>();
	
	public GameFrame(String title) {
		super(title);
		setResizable(false);
		setSize(GAME_WIDTH, GAME_HEIGHT);
		lastRender = -1;
		ThreadPool.init();
		fpsHistory = new ArrayList<>(100);

		try{
			tankBody = ImageIO.read(new File("tankBody.png"));
			tankGun = ImageIO.read(new File("tankGun.png"));
			tankGun = ImageIO.read(new File("tankGun.png"));
			opponentTank = ImageIO.read(new File("opponentTank.png"));
			opponentTankGun = ImageIO.read(new File("opponentTankGun.png"));
		}
		catch(IOException e){
			System.out.println(e);
		}

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getButton() == MouseEvent.BUTTON1) {
					int firingLocXOfBullet = (int) ( GameState.tankCenterX + 75 * Math.cos(tankGunAngle));
					int firingLocYOfBullet = (int) (GameState.tankCenterY + 75 * Math.sin(tankGunAngle));

						bullets.add(new Bullet(firingLocXOfBullet,
								firingLocYOfBullet , tankGunAngle));

				/*	if (GameState.tankDirection == GameState.LEFT) {
						bullets.add(new Bullet(GameState.tankCenterX - 100,
								GameState.tankCenterY + Bullet.bulletHeight / 2, tankGunAngle));
					}
					if (GameState.tankDirection == GameState.UP) {
						bullets.add(new Bullet(GameState.tankCenterX ,
								GameState.tankCenterY - 100, tankGunAngle));
					}
					if (GameState.tankDirection == GameState.DOWN) {
						bullets.add(new Bullet(GameState.tankCenterX,
								GameState.tankCenterY + 100, tankGunAngle));
					} */

				}
			}
		});

	}
	
	/**
	 * This must be called once after the JFrame is shown:
	 *    frame.setVisible(true);
	 * and before any rendering is started.
	 */
	public void initBufferStrategy() {
		// Triple-buffering
		createBufferStrategy(3);
		bufferStrategy = getBufferStrategy();
	}

	
	/**
	 * Game rendering with triple-buffering using BufferStrategy.
	 */
	public void render(GameState state) {
		// Render single frame
		do {
			// The following loop ensures that the contents of the drawing buffer
			// are consistent in case the underlying surface was recreated
			do {
				// Get a new graphics context every time through the loop
				// to make sure the strategy is validated
				Graphics2D graphics = (Graphics2D) bufferStrategy.getDrawGraphics();
				try {
					doRendering(graphics, state);
				} finally {
					// Dispose the graphics
					graphics.dispose();
				}
				// Repeat the rendering if the drawing buffer contents were restored
			} while (bufferStrategy.contentsRestored());

			// Display the buffer
			bufferStrategy.show();
			// Tell the system to do the drawing NOW;
			// otherwise it can take a few extra ms and will feel jerky!
			Toolkit.getDefaultToolkit().sync();

		// Repeat the rendering if the drawing buffer was lost
		} while (bufferStrategy.contentsLost());
	}
	
	/**
	 * Rendering all game elements based on the game state.
	 */
	private void doRendering(Graphics2D g2d, GameState state) {

        // draw the map
		a.designMap();
        a.drawMap(g2d,state.cameraY);


        // Drawing the rotated image at the required drawing locations
        if (ab.showTank) {
            g2d.drawImage(ab.enemyImg, 500, 500, null);
            ab.collide();
        }

        tankGunAngle = Math.atan2((state.aimY - state.tankCenterY),(state.aimX - state.tankCenterX ));

        g2d.drawImage(rotatePic(tankBody, state.tankBodyAngle),state.tankCenterX - 90,state.tankCenterY - 90,null);

        g2d.drawImage(rotatePic(tankGun, tankGunAngle), state.tankCenterX - 90, state.tankCenterY -90, null);

        updateBulletsState(g2d);





        // 	g2d.drawImage(tankGun,state.locX,state.locY,null);

			g2d.setColor(Color.RED);
			g2d.fillOval(state.tankCenterX, state.tankCenterY,10, 10);


		// Print FPS info
		long currentRender = System.currentTimeMillis();
		if (lastRender > 0) {
			fpsHistory.add(1000.0f / (currentRender - lastRender));
			if (fpsHistory.size() > 100) {
				fpsHistory.remove(0); // remove oldest
			}
			float avg = 0.0f;
			for (float fps : fpsHistory) {
				avg += fps;
			}
			avg /= fpsHistory.size();
			String str = String.format("Average FPS = %.1f , Last Interval = %d ms, angle = %f, tankX = %d, tankY = %d,aimX = %d, aimY = %d, direction = %d" +
							"cameraY = %d",
					avg, (currentRender - lastRender),tankGunAngle,state.tankCenterX,state.tankCenterY,state.aimX,state.aimY, state.tankDirection, state.cameraY);
			g2d.setColor(Color.CYAN);
			g2d.setFont(g2d.getFont().deriveFont(18.0f));
			int strWidth = g2d.getFontMetrics().stringWidth(str);
			int strHeight = g2d.getFontMetrics().getHeight();
			g2d.drawString(str, (GAME_WIDTH - strWidth) / 2, strHeight+50);
		}
		lastRender = currentRender;
		// Print user guide
		String userGuide
				= "Use the MOUSE or ARROW KEYS to move the BALL. "
				+ "Press ESCAPE to end the game.";
		g2d.setFont(g2d.getFont().deriveFont(18.0f));
		g2d.drawString(userGuide, 10, GAME_HEIGHT - 10);
		// Draw GAME OVER
		if (state.gameOver) {
			String str = "GAME OVER";
			g2d.setColor(Color.WHITE);
			g2d.setFont(g2d.getFont().deriveFont(Font.BOLD).deriveFont(64.0f));
			int strWidth = g2d.getFontMetrics().stringWidth(str);
			g2d.drawString(str, (GAME_WIDTH - strWidth) / 2, GAME_HEIGHT / 2);
		}
	}

	public BufferedImage rotatePic (BufferedImage img, double angle) {
        double locationX = img.getWidth() / 2;
        double locationY = img.getHeight() / 2;
        AffineTransform tx = AffineTransform.getRotateInstance(angle, locationX , locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        return op.filter(img,null);
    }

    public static Rectangle mainTankRectangle () {
	    return new Rectangle((int) GameState.tankCenterX - 90,(int) GameState.tankCenterY - 90,150,150);
    }

    public void updateBulletsState (Graphics2D g2d) {
		for (int i = 0; i < bullets.size(); i++) {
			g2d.drawImage(rotatePic(bullets.get(i).getBulletImg(),bullets.get(i).bulletAngle),(int) bullets.get(i).bulletCenterLocX,(int) bullets.get(i).bulletCenterLocY,null);
			bullets.get(i).moveBullet();
			bullets.get(i).checkForBulletCollision(g2d);
			if (bullets.get(i).isRemoved())
				bullets.remove(i);
		}
	}


}
