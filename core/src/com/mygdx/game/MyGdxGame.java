package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.backends.lwjgl.audio.Mp3.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.*;
import com.badlogic.gdx.InputMultiplexer;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTextField;



public class MyGdxGame extends ApplicationAdapter {
	protected int state;
	
	//Hintergrund Schwimmwelt
	private Sprite wellen1;
	private Sprite wellen2;
	private Sprite tauchersprite;
	private Sprite ufer_links;
	private Sprite ufer_rechts;
	
	//Hintergrund Tauchwelt
	private Sprite hintergrund1;
	private Sprite hintergrund2;
	private Sprite hintergrund3;
	private Sprite hintergrund4;

	private Sprite herz_leer;
	private Sprite herz_voll;
	private Sprite swimmer;
	private Sprite swimmer_rechter_arm;
	private Sprite swimmer_linker_arm;
	
	private SpriteBatch batch;
	
	private World world;
	public Body body;
		
	// Schrift
	private BitmapFont font;
	private BitmapFont gameover; 

	//Graphics Updates -> Variables to update positions
	private float wellen_x_pos;
	private float loop;
	private float unter_wasser_textur_pos;
	private float zeit_unter_wasser;
	private float hindernis_x_pos;

	//Hindernis-Array-Swim
	private Obstacle[] hindernis = new Obstacle[40];
	//Positionen aktiver Hindernisse in array
	private boolean[] hindernis_aktiv = new boolean[40];
	
	// Hindernis Array Dive
	private float[] wand_punkte = new float[2*10];
	
	//Hilfsvariable für den Hindernisgenerator
	//Bei Aufruf von Hindernis-Generator wird h auf 0 gesetzt
	//Bei jedem Aufruf von render wird geschwindigkeit auf h addiert
	//Bis h größer gleich der Länge eines Hindernisses ist, dann starte Hindernisgenerator
	private float h;
	
	// Variablen für Schwimmer, Hintergrund, Hindernis
	private float geschwindigkeit;
	private float hindernis_geschwindigkeit;
	//Aenderung der Geschwindigkeit
	private float beschleunigung;
	
	//swimmer variables
	//Bahn des Schwimmers
	private int swimmer_position_swim;
	//swimmer Groesse
	private float swimmer_width;
	private float swimmer_height;
	//Abstand zur Bahn
	private float swimmer_offset;
	//Position der Arme
	private float arm_pos_x = 0.0f;
	private float arm_pos_y = 0.0f;

	// game variables
	private long score;
	private long level;
	private int health;

	// Zählt wie viel weiter geschwommen wurde, in Länge eines Hindernisses 
	private long Zeile; 
	
	//Musik & Sound
	private Sound sound; 
	private Music music; 
	private Music bewegungmusic;
	private Music shark;

	// shortcuts for graphics fields
	private int width, height;
	private float ppiX, ppiY;
	private float width2; 
	// input
	private boolean paused;
	private InputMultiplexer multiplexer;

	private Menu menu;	
	private EventListener steuerung;

	
	//Kollisionserkennung -> TODO: Ohne Variable loesen
	private boolean accident;
	
	//Luftanzeige
	private Sprite luftanzeige;


	@Override
	public void create () {
		
		
		//init Sounds
		music = Gdx.audio.newMusic(Gdx.files.internal("super-mario-bros.mp3"));
		music.setLooping(true);
		music.setVolume(0.3f);
		shark = Gdx.audio.newMusic(Gdx.files.internal("shark_bite.mp3"));
		shark.setVolume(0.3f);
		
		//init state
		state = 0;
		
		//Infos Screen;
		readGraphics();
		
		//New Sprite Batch
		batch = new SpriteBatch();
		
		//init Wellentextur
		wellen1 = new Sprite(new Texture("wellen.png"));
		wellen1.setSize(width, height);
		wellen2 = new Sprite(new Texture("wellen.png"));
		wellen2.setSize(width, height);
		wellen_x_pos = 0;
		loop = 0;
		
		//init Unterwasserwelt Hintergrund
		hintergrund1 = new Sprite(new Texture("unter_wasser_textur_1.png"));
		hintergrund1.setSize(width, height);
		hintergrund2 = new Sprite(new Texture("unter_wasser_textur_2.png"));
		hintergrund2.setSize(width, height);
		hintergrund3 = new Sprite(new Texture("unter_wasser_textur_3.png"));
		hintergrund3.setSize(width, height);
		hintergrund4 = new Sprite(new Texture("unter_wasser_textur_4.png"));
		hintergrund4.setSize(width, height);
		unter_wasser_textur_pos = 0.0f;
		zeit_unter_wasser = 0.0f;

		//Luftanzeige
		luftanzeige = new Sprite(new Texture("image.png"));
		luftanzeige.setSize(width/18, height/18);
		
		//init Taucher
		tauchersprite = new Sprite(new Texture("schwimmer_aufsicht.png"));
		
		world = new World(new Vector2(0, 5), true);
		BodyDef diver = new BodyDef();
		diver.type = BodyDef.BodyType.DynamicBody;
		
		diver.position.set(0,0);
		body = world.createBody(diver);
		
		CircleShape circle = new CircleShape();
		circle.setRadius(6f);
		
		FixtureDef diverfixture = new FixtureDef();
		diverfixture.shape = circle;
		diverfixture.density = 0.1f;
		diverfixture.friction = 0.4f;
		diverfixture.restitution = 0.6f;
		
		body.createFixture(diverfixture);
		
		circle.dispose();

		//Anzeigen
		//init Lebens-Anzeige
		herz_leer = new Sprite(new Texture("herz_leer.png"));
		herz_voll = new Sprite(new Texture ("herz_voll.png"));		
		herz_voll.setSize(width/18, height/18);
		herz_leer.setSize(width/18, height/18);
		
		health = 5;
		
		
		//init Schrift für alle Anzeigen
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Mecha_Bold.ttf"));
		FreeTypeFontParameter parameter1 = new FreeTypeFontParameter();
		FreeTypeFontParameter parameter2 = new FreeTypeFontParameter();
		parameter1.size = 27;
		parameter2.size = 50;
		parameter1.characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.!'()>?: ";
		font = generator.generateFont(parameter1);
		gameover = generator.generateFont(parameter2);
		
		//init Swimmer_Grafik
		swimmer = new Sprite(new Texture("schwimmer_aufsicht_body.png"));
		swimmer_linker_arm = new Sprite(new Texture("schwimmer_aufsicht_linker_arm.png"));
		swimmer_rechter_arm = new Sprite(new Texture("schwimmer_aufsicht_rechter_arm.png"));
		
		swimmer_offset = ((width-2) / 9) * 1/8;
		swimmer_width = ((width-2) / 9) * 3/4;
		swimmer_height= ((width-2)/ 9) * 3/4;

		//init Ufertextur
		ufer_links = new Sprite(new Texture("ufer.png"));
		ufer_links.setSize(width/9, height);
		ufer_rechts = new Sprite(new Texture("ufer.png"));
		ufer_rechts.setSize(width/9, height);
		ufer_rechts.flip(true, false);
		ufer_rechts.setOrigin(width - ufer_rechts.getWidth(), 0);		
	
		//init geschwindigkeit
		geschwindigkeit = 1.0f;
		beschleunigung = 0;
		
		//init swimmer_position
		swimmer_position_swim = 4;
		
		//init score
		score = 0;
		level = 1;
		
		//input
		paused = false;
		multiplexer = new InputMultiplexer();
		// erstelle menu
		menu = new Menu(multiplexer, this, font);
		menu.loadMainMenu();
		
		//erstelle und registriere Steuerung
		steuerung = new EventListener();
		steuerung.setGame(this);
		multiplexer.addProcessor(steuerung);
		Gdx.input.setInputProcessor(multiplexer);
	}

	
	@Override 
	public void render () {

		
		if(state == 0 || paused){
			menu.render();
		}

		//TODO: Speicherplatz von Hindernissen mit hindernis.dispose() freigeben!
		if(!paused){
			if(state == 1){
				render_upperworld();
				update_variables();}
			if(state == 2){
				render_lowerworld();
				update_variables_dive();}			
			//Graphik-Variablem updaten
			update_graphics();}

	}
	
	// Methode um die Schwimmwelt zu rendern	
	private void render_upperworld(){
		
		//Musik 
		music.play();
		
		if (h >= width/9){
		Hindernis_Generator();
		score++;}
		h += geschwindigkeit;
		// Hindernisse bewegen
				
		// Kollisionsabfrage
		for (int i=0; i<40; i++){
			if (hindernis_aktiv[i]){
				if (meetObstacle(hindernis[i],swimmer)){
					shark.play();
					health--;
					hindernis[i].dispose();
				    hindernis_aktiv[i]=false;	
				}
			}
		}
		
		//Hintergrundfarbe
		Gdx.gl.glClearColor(0, 0.6f, 0.9f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			
		batch.begin();
				
		//Hintergrund		
		batch.draw(wellen1, 0, wellen_x_pos % height, width, height);
		batch.draw(wellen2, 0, (wellen_x_pos % (height)) + height, width, height);
		batch.draw(ufer_links, 0, 0, width/9, height);
		batch.draw(ufer_rechts, ufer_rechts.getOriginX(), ufer_rechts.getOriginY(), width/9, height);


		//Animation Schwimmer
		batch.draw(swimmer_rechter_arm, (width-2*width/9) / 7 * (swimmer_position_swim-1) + swimmer_offset + width/9 + swimmer_width/5.0f + (swimmer_width/5.5f) - arm_pos_x*swimmer_width/70, (swimmer_width/4.5f - arm_pos_y*swimmer_width/80), swimmer_width/2, swimmer_width);
		batch.draw(swimmer_linker_arm, (width-2*width/9) / 7 * (swimmer_position_swim-1) + swimmer_offset + width/9 + swimmer_width/5.0f - (swimmer_width/5.5f) + arm_pos_x*swimmer_width/70, (swimmer_width/4.5f - arm_pos_y*swimmer_width/80), swimmer_width/2, swimmer_width);
		batch.draw(swimmer, (width-2*width/9) / 7 * (swimmer_position_swim-1) + swimmer_offset + width/9, 0, swimmer_width, swimmer_width);
		

		//Hindernisse
		for(int i = 0; i<40; i++){
			if(hindernis_aktiv[i]){
				Obstacle aktiv = hindernis[i];
				int aktiv_type = aktiv.getType();
				switch(aktiv_type){
					case 0:
					case 1:
						batch.draw(aktiv.getSprite(), (width/9)*aktiv.getBahn(), height - aktiv.getY(), width/9, width/9);	
						break;
					case 2:
						batch.draw(aktiv.getSprite(), (width/9)*aktiv.getBahn(), height - aktiv.getY(), width/9, width/9);
						batch.draw(aktiv.getSpritesAnim()[0], (width/9)*aktiv.getBahn() + 40 + (aktiv.getY()%10), height - aktiv.getY() + (width/9)/15, width/18, width/18);						
						break;
					case 3:	
						batch.draw(aktiv.getSprite(), (width/9)*aktiv.getBahn(), height - aktiv.getY(), width/9, width/9);	
						break;
					default:
						batch.draw(aktiv.getSprite(), (width/9)*aktiv.getBahn(), height - aktiv.getY(), width/9, width/9);	
						break;
				}
			}
		}
		
		
		//Score-Anzeige
		font.setColor(Color.BLACK);
		font.draw(batch, "Score: " + score, 470, 465);
		
		//Level-Anzeigen 
		if (score%30 < 2){
			gameover.draw(batch, "Level " + level, width/2, height/2);
		}
		
		//Luft-Anzeige
		width2 = width/2 + (loop*0.5f);
		if (width2 > 0){
			batch.draw(luftanzeige, 40, 40, width2, height/18);
			}
		else {gameover.draw(batch, "GAME OVER", width /2, height/2);	
		gameover.setColor(Color.WHITE);
			geschwindigkeit = 0;
			music.stop();
		}
		
		// Herzen update
		if (health == 5){
			batch.draw(herz_voll, 19, 440, width/18, height/18);
			batch.draw(herz_voll, 55, 440, width/18, height/18);
			batch.draw(herz_voll, 90, 440, width/18, height/18);
			batch.draw(herz_voll, 125, 440, width/18, height/18);
			batch.draw(herz_voll, 160, 440, width/18, height/18);
						
		} else if (health == 4) {
			batch.draw(herz_voll, 19, 440, width/18, height/18);
			batch.draw(herz_voll, 55, 440, width/18, height/18);
			batch.draw(herz_voll, 90, 440, width/18, height/18);
			batch.draw(herz_voll, 125, 440, width/18, height/18);
			batch.draw(herz_leer, 160, 440, width/18, height/18);
							
		}else if (health == 3) {
			batch.draw(herz_voll, 19, 440, width/18, height/18);
			batch.draw(herz_voll, 55, 440, width/18, height/18);
			batch.draw(herz_voll, 90, 440, width/18, height/18);
			batch.draw(herz_leer, 125, 440, width/18, height/18);
			batch.draw(herz_leer, 160, 440, width/18, height/18);
						
						
		}else if (health == 2) {
			batch.draw(herz_voll, 19, 440, width/18, height/18);
			batch.draw(herz_voll, 55, 440, width/18, height/18);
			batch.draw(herz_leer, 90, 440, width/18, height/18);
			batch.draw(herz_leer, 125, 440, width/18, height/18);
			batch.draw(herz_leer, 160, 440, width/18, height/18);
				
		}else if (health == 1) {
			batch.draw(herz_voll, 19, 440, width/18, height/18);
			batch.draw(herz_leer, 55, 440, width/18, height/18);
			batch.draw(herz_leer, 90, 440, width/18, height/18);
			batch.draw(herz_leer, 125, 440, width/18, height/18);
			batch.draw(herz_leer, 160, 440, width/18, height/18);
		}
		
		else if (health == 0) {
		batch.draw(herz_leer, 19, 440, width/18, height/18);
		batch.draw(herz_leer, 55, 440, width/18, height/18);
		batch.draw(herz_leer, 90, 440, width/18, height/18);
		batch.draw(herz_leer, 125, 440, width/18, height/18);
		batch.draw(herz_leer, 160, 440, width/18, height/18);
		}
				
		// Game Over Anzeige
		else {gameover.draw(batch, "GAME OVER", width /2, height/2);	
		gameover.setColor(Color.WHITE);
		batch.draw(herz_leer, 19, 440, width/18, height/18);
		batch.draw(herz_leer, 55, 440, width/18, height/18);
		batch.draw(herz_leer, 90, 440, width/18, height/18);
		batch.draw(herz_leer, 125, 440, width/18, height/18);
		batch.draw(herz_leer, 160, 440, width/18, height/18);
		geschwindigkeit = 0;
		music.stop();}
		
		batch.end();
		}
		

	
	// Methode um die Tauchwelt zu rendern	
	private void render_lowerworld(){
		//Hintergrundfarbe
		Gdx.gl.glClearColor(0.6f, 0.6f, 0.9f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				
		world.step(Gdx.graphics.getDeltaTime(), 6, 2);

		

		
		ShapeRenderer wand = new ShapeRenderer();
		wand.setColor(Color.GRAY);
		
		wand.begin(ShapeType.Line);
		
		// TODO for ...
		
		wand.end();
		
		
		batch.begin(); 	
        
		
        //Hintergrundanimation
  		batch.draw(hintergrund1, 0, -10 - unter_wasser_textur_pos, width, height - height/8);
  		batch.draw(hintergrund2, 0, -10 - 3*unter_wasser_textur_pos, width, height - height/8);
  		batch.draw(hintergrund3, 0, -10 - 7*unter_wasser_textur_pos, width, height - height/8);
  		batch.draw(hintergrund4, 0, -10 - 10*unter_wasser_textur_pos, width, height - height/8);
  		
  		// Taucher
        batch.draw(tauchersprite, tauchersprite.getX(), tauchersprite.getY());
        
        
        batch.end();
		
	}
	
	//Helpermethods
	
	private void Hindernis_Generator(){
		h = 0;
		//erste einfache Version des Hindernisgenerators
		//erstellt ein zufälliges Hindernis von Typ 1-3 auf einer zufälligen Bahn mit 50%iger Wahrscheinlichkeit
		if (Math.random()<0.5){
		int random_bahn = (int)(Math.random()*7+1);
		int random_hindernis = (int)(Math.random()*3);
		int i = 0;
		while (hindernis_aktiv[i]){
			i++;
		}
		hindernis[i] = init_obstacle(random_hindernis,random_bahn);
		hindernis_aktiv[i]=true;
		}
	}
	
	private void Hindernis_Generator_dive(){
		
		for(int i=0; i<18; i++){
			
			wand_punkte[i] = wand_punkte[i+2];
			
		}
		
		float w0 = 100*(float)Math.random()+100;
		float w1 = 100*(float)Math.random();
		
		while(w0-w1<100){
			
			w0 = 100*(float)Math.random()+100;
			w1 = 100*(float)Math.random();
			
		}
		
		wand_punkte[18] = w0;
		wand_punkte[19] = w1;
		
	}
	
	public int getState(){
		return state;
	}
	
	public void startGame(){
		//TODO: init game variables
		state = 1;
	}
	
	public void pauseGame(boolean p){
		if(p && p != paused){
			menu.loadPauseMenu();
		}
		paused = p;
	}
	
	public void returnToMainMenu(){
		//TODO: cleanup ?
		paused = false;
		menu.loadMainMenu();
		state = 0;
	}
	
	public void endApplication(){
		Gdx.app.exit();
	}
	
	public void changeDiveState(){
		
		if(state == 1){
			state = 2;
			body.setLinearVelocity(0, 0);
			body.setTransform(0, 100, 0);
			
			// TODO Dispose einfügen
		}
		else{
			state = 1;
		}
		
	}
		
	protected void changeSwimmerPosition_swim(int change){
		swimmer_position_swim += change;	
		if(swimmer_position_swim < 1){
			swimmer_position_swim = 1;
		}
		if(swimmer_position_swim > 7){
			swimmer_position_swim = 7;
		}
	}
	
	protected void changeSwimmerPosition_dive(int change){
				
		body.applyForceToCenter(0, change, true);
		
	}
	
	public boolean meetObstacle(Obstacle obs, Sprite swimmer){
		if(swimmer_position_swim == obs.getBahn()){
		if(width*8/9-obs.getY()<2.5*swimmer_height){
				return true;
			}
		}
		return false;
	}
	

	private void update_graphics(){

		if(state == 1){
			wellen_x_pos -= geschwindigkeit;
			loop -= geschwindigkeit;
			arm_pos_x += Math.sin(0.1*wellen_x_pos -0.5);
			arm_pos_y -= Math.sin(0.1*wellen_x_pos);
			//Update Hindernisse
			for(int i = 0; i<40; i++){
				if(hindernis_aktiv[i]){
					Obstacle aktiv = hindernis[i];
					int aktiv_type = aktiv.getType();
					switch(aktiv_type){
						case 0:
						case 1:
						case 2: 
							aktiv.setY(aktiv.getY() + geschwindigkeit);
							break;
						case 3:
							//Richtungswechsel?
							
							//Bahn wechseln -> nach rechts oder nach links? 
							if(aktiv.getY()%10 == 0 && aktiv.getRichtung() == 1) {
								//Richtungswechsel
								if(aktiv.getBahn()==7) {
									aktiv.setRichtung(2);
									Sprite temp = aktiv.getSprite();
									temp.flip(true, false);
									aktiv.setSprite(temp);
								}
								else aktiv.setBahn(aktiv.getBahn()+1);
							}
							else if(aktiv.getY()%10 == 0 && aktiv.getRichtung() == 2) {
								//Richtungswechsel
								if(aktiv.getBahn()==1) {
									aktiv.setRichtung(1);
									Sprite temp = aktiv.getSprite();
									temp.flip(true, false);
									aktiv.setSprite(temp);
								}
								else aktiv.setBahn(aktiv.getBahn()-1);
							}
							aktiv.setY(aktiv.getY() + geschwindigkeit);
							break;
						default:
							batch.draw(aktiv.getSprite(), (width/9)*aktiv.getBahn(), height - aktiv.getY(), width/9, width/9);	
							break;
					}
					//Wenn Hindernis Fenster verlassen hat -> dispose
					if(aktiv.getY() > (height + height/9)){
						aktiv.dispose();
						hindernis_aktiv[i] = false;
					}
				}
			}
		}
		else if(state == 2){
			//Bewegung Hintergrundtextur
			unter_wasser_textur_pos = ((float) Math.sin((double)0.05f* zeit_unter_wasser));
			//if(zeit_unter_wasser < 100 ) unter_wasser_textur_pos = (float) (unter_wasser_textur_pos + 0.1); 
			//else if(zeit_unter_wasser > 99) unter_wasser_textur_pos = (float) (unter_wasser_textur_pos - 0.1);
			zeit_unter_wasser = (zeit_unter_wasser + 1)%200;
			
			// Bewegung Hindernisse
			hindernis_x_pos -= hindernis_geschwindigkeit;
		}


	}
	
	private void readGraphics() {
		width = Gdx.graphics.getWidth();
		height= Gdx.graphics.getHeight();
		ppiX = Gdx.graphics.getPpiX();
		ppiY = Gdx.graphics.getPpiY();

	}
	
	
	private void update_variables() {
		geschwindigkeit += beschleunigung;
		swimmer_offset = ((width-2) / 9) * 1/8;
		swimmer_width = ((width-2) / 9) * 3/4;
		swimmer_height= ((width-2)/ 9) * 3/4;
		width2 = luftanzeige.getHeight ();
		}
	
	
	private void update_variables_dive(){
		
	tauchersprite.setPosition(body.getPosition().x, body.getPosition().y);
		
		if(body.getPosition().y > 200){
			changeDiveState();
		}
		if(body.getPosition().y < 0){
			body.setLinearVelocity(0, 0);
			body.setTransform(0, 0, 0);
		}
		
	}

	//init Klasse, um Obstacle-Objekte zu erzeugen 
	private Obstacle init_obstacle (int type, int bahn){
		Obstacle new_obstacle;

		
		switch(type){
			case 0: 
				Sprite felsen_sprite = new Sprite(new Texture("hindernis_felsen.png"));
				felsen_sprite.setSize(width/9, height/9);
				new_obstacle = new Obstacle(felsen_sprite, 0, bahn, 0.0f);
				break;
			case 1:
				Sprite seerosen_sprite = new Sprite(new Texture("seerosen.png"));
				seerosen_sprite.setSize(width/9, height/9);
				new_obstacle = new Obstacle(seerosen_sprite, 1, bahn, 0.0f);
				break;
			case 2:
				Sprite hai_sprite = new Sprite(new Texture("hai_1.png"));
				hai_sprite.setSize(width/9, height/9);
				Sprite haikinn = new Sprite(new Texture("hai_2.png")); 
				Sprite[] sprites_anim = new Sprite[1];
				sprites_anim[0] = haikinn;
				new_obstacle = new Obstacle(hai_sprite, 2, bahn, 0.0f, 1, sprites_anim);
				break;
			case 3:
				Sprite schwan_sprite = new Sprite(new Texture("rennschwan.png"));
				schwan_sprite.setSize(width/9, height/9);
				new_obstacle = new Obstacle(schwan_sprite, 3, bahn, 0.0f);
				//Richtung auf links setzen
				new_obstacle.setRichtung(2);
				break;
			default: 
				Sprite default_sprite = new Sprite(new Texture("hindernis_felsen.png"));
				default_sprite.setSize(width/9, height/9);
				new_obstacle = new Obstacle(default_sprite, 0, bahn, 0.0f);
				break;
		}
		return new_obstacle;
		
		
	}

	@Override
	public void dispose() {
		music.dispose();
		//generator.dispose();
	}

}
