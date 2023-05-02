package com.mycompany.eksamen;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


     /**
     * @author?
     */
public class App extends Application {
    
    static int id;
    static int iRom;
    
    static int port = 8000;
    static String host = "192.168.10.158";
    static ObjectOutputStream out;
    static ObjectInputStream in;
    static Socket socket;
    
    final static ObservableList romTab = FXCollections.observableArrayList();
    final static ArrayList meldingTab = new ArrayList<>();
    final int BREDDE = 700;
    final int HØYDE = 600;
    static int selected = 100;
    static int next = 100;
    
    static boolean siste = false;
    static boolean videre = false;
    
    static BorderPane startOmråde = new BorderPane();
    static BorderPane tegneOmråde = new BorderPane();
    static Button sendKnapp = new Button("Send");
    static Button forlatKnapp = new Button("Forlat Rommet");
    static Button opprettKnapp = new Button("Opprett Rom");
    static Button ferdig = new Button("Ferdig");
    static TextArea skriveFelt = new TextArea();
    static TextField brukernavnInput = new TextField();
    
    Alert alert2 = new Alert(Alert.AlertType.CONFIRMATION);
    static ListView romListe = new ListView(romTab);
    static VBox meldinger = new VBox();
    static Label mld = new Label();
    static ScrollPane scroll = new ScrollPane();  
    static HBox skriveWrapper = new HBox();
    
    ExecutorService es;
    
    Scene startScene = new Scene(startOmråde, BREDDE, HØYDE);
    Scene hovedScene = new Scene(tegneOmråde, BREDDE, HØYDE);
    
    /**
    * start - Lager GUI 
    */
    @Override
    public void start(Stage stage) throws IOException, ClassNotFoundException {
        scroll.setVvalue(1.0);
        tegnStart();
        
        //LAG STØRRELSER MED BIND
        startOmråde.prefHeightProperty().bind(stage.heightProperty());
        startOmråde.prefWidthProperty().bind(stage.widthProperty());
        tegneOmråde.prefHeightProperty().bind(stage.heightProperty());
        tegneOmråde.prefWidthProperty().bind(stage.widthProperty());
                 
        //scroll.prefWidthProperty().bind(stage.widthProperty());
        //scroll.prefHeightProperty().bind(stage.heightProperty().divide(2));
        //meldinger.prefWidthProperty().bind(scroll.widthProperty());
        //meldinger.prefHeightProperty().bind(scroll.heightProperty());
        //mld.prefHeightProperty().bind(meldinger.heightProperty().divide(10));
        //mld.prefWidthProperty().bind(meldinger.widthProperty());
        
        romListe.prefHeightProperty().bind(stage.heightProperty());
        romListe.prefWidthProperty().bind(stage.widthProperty().divide(5));
        skriveWrapper.prefWidthProperty().bind(stage.widthProperty());
        skriveFelt.prefHeightProperty().bind(stage.heightProperty().divide(11));
        skriveFelt.prefWidthProperty().bind(stage.widthProperty().divide(1.5));
        opprettKnapp.prefWidthProperty().bind(romListe.widthProperty().divide(2));
        opprettKnapp.prefHeightProperty().bind(skriveFelt.heightProperty());
        forlatKnapp.prefHeightProperty().bind(skriveFelt.heightProperty());
        forlatKnapp.prefWidthProperty().bind(romListe.widthProperty().divide(2));
        sendKnapp.prefHeightProperty().bind(skriveFelt.heightProperty());
        sendKnapp.prefWidthProperty().bind(stage.widthProperty().subtract(opprettKnapp.prefWidthProperty().add(skriveFelt.prefWidthProperty()).add(forlatKnapp.prefWidthProperty())));
        
       
        sendKnapp.setOnAction(e ->{
            
             //Send melding 
            try {
                sendMelding();
                clearText();
                tegnChat();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });
        
        forlatKnapp.setOnAction(e ->{
            try {
                //Forlat chatterom
                sjekkSiste();
                if(siste &&  videre){
                    int closingRom = iRom;
                    fjernRom(closingRom);
                    iRom = 0;
                    skriveFelt.setText("");
                    skriveFelt.setPromptText("Skriv noe...");
                    out.writeObject(8);
                }
                else if(siste && !videre){
                    // blir i rom
                }
                else{
                    iRom = 0;
                    skriveFelt.setText("");
                    skriveFelt.setPromptText("Skriv noe...");
                    out.writeObject(8);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });        
        
        opprettKnapp.setOnAction(e ->{
          
            //Opprett chatterom 
            //hentet fra https://stackoverflow.com/questions/22166610/how-to-create-a-popup-windows-in-javafx
            final Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stage);
            dialog.setTitle("Opprett rom");
            VBox dialogVbox = new VBox(20);
            Label skriv = new Label("Skriv navn:");
            TextField romNavn = new TextField();
            Button knapp = new Button("Ferdig");
            skriv.setGraphic(romNavn);
            skriv.setContentDisplay(ContentDisplay.BOTTOM);
            dialogVbox.getChildren().addAll(skriv, knapp);
            dialogVbox.setAlignment(Pos.CENTER);
            Scene dialogScene = new Scene(dialogVbox, 300, 200);
            dialog.setScene(dialogScene);
            dialog.show();
            romNavn.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent keyEvent) {
                    if (keyEvent.getCode() == KeyCode.ENTER)  {
                        String text = romNavn.getText();
                        if (romNavn.getText().isEmpty()) {
                
                        } else {
                            knapp.fire();
                            dialog.close();
                        }
                    
                    }
                }
            });
            knapp.setOnAction(ev ->{
                if (romNavn.getText().isEmpty()) {
                } else {
                    try {
                        sjekkSiste();
                        if(siste && videre){
                            int closingRom = iRom;
                            leggTilRom(romNavn.getText());
                            fjernRom(closingRom);
                            tegnChat();
                        }else if(siste && !videre){
                            System.out.println("Lager nytt rom");
                        } else{
                            System.out.println("Lager nytt rom");
                            leggTilRom(romNavn.getText());
                            tegnChat();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (ClassNotFoundException ex) {
                        ex.printStackTrace();
                    }
                    //Send romnavn videre
                    dialog.close();
                }
            });
        });
        
        ferdig.setOnAction(e ->{  
        
            if (brukernavnInput.getText().isEmpty()) {
                
                } else {
                try {
                    leggTilBruker(brukernavnInput.getText());
                    iRom = 0;
                    //Send text videre
                    stage.setScene(hovedScene);
                    fåAlleAktiveRom();
                    tegnRom();
                    
                    tegnSkriveOmråde();
                    
                    es = Executors.newFixedThreadPool(2);
                    System.out.println("Starter threads");
                    es.execute(new UpdateThread(3, socket));
                    es.execute(new UpdateThread(5, socket));
                    
                    es.shutdown();
                    
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
                    
                }
            
        });       
        
        stage.setScene(startScene);
        //stage.setResizable(false);
        stage.show();
        
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                Platform.exit();
                try {
                    // On exit
                    out.writeObject(69);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                es.shutdown();
                System.exit(0);
            }
        });
        
    }

    public static void main(String[] args) {
        launch();
    }
    /**
    * tegnRom - Legger til rom i ListView og går inn i rommet hvis man trykker på rommet/sletter forrige rom hvis bruker er siste.
    */
    public static void tegnRom() {
        //HVILKET ROM?!
        skriveFelt.setDisable(true);
        romListe.setOnMouseClicked(event ->{
            
            Rom nyttRom = (Rom)romListe.getSelectionModel().getSelectedItem();
            if(iRom == 0)
                skriveFelt.setDisable(true);
                
            else if (iRom == nyttRom.id) {
                
            }else{
                skriveFelt.setDisable(false);
                try {
                    sjekkSiste();
                    if(siste && videre){
                        
                        System.out.println("Skal videre");
                        
                        byttTilRom(nyttRom);
                        
                        int closingRom = iRom;
                        iRom = nyttRom.id;
                        fjernRom(closingRom);
                        tegnChat();
                         
                    }else if(siste && !videre){
                        // Går ikke videre
                    }
                    else{
                        
                        byttTilRom(nyttRom);
                        iRom = nyttRom.id;
                        tegnChat();
                        
                    }
                    
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
                
            }
        });
        //forlatKnapp.setGraphic(opprettKnapp);
        //forlatKnapp.setContentDisplay(ContentDisplay.LEFT);
        tegneOmråde.setLeft(romListe);
        
    }
    
    /**
    * tegnChat - Henter og skriver meldinger og legger de til i VBox. Gjør skrivefelt disabled hvis bruker ikke er i et rom.
    */
    public static void tegnChat() throws IOException, ClassNotFoundException {
        //HVILKE MELDINGER?!
        System.out.println("Skriver ut chatmeldinger");
        
        meldinger.getChildren().clear();
        
        out.writeObject(5);
        System.out.println("Spør om tabeller");
        out.writeObject(iRom);
        
        String [] mldTab = (String[])in.readObject();
        System.out.println("Motatt meldinger");
        String[] navnTab = (String[])in.readObject();
        int[] idTab = (int[])in.readObject();
        String[] tidTab = (String[])in.readObject();
        System.out.println("Tabeller motatt");
        
        if (iRom == 0) {
            skriveFelt.setDisable(true);
        } else {
            System.out.println("Skriver meldinger");
            for (int i = 0; i < mldTab.length; i++) {

                String melding = mldTab[i];
                String tid = tidTab[i];
                int avsenderId = idTab[i];
                String avsender = navnTab[i];

                skriveFelt.setDisable(false);

                mld = new Label(melding);
                Label sender = new Label(avsender);
                sender.setGraphic(new Label(tid));
                sender.setContentDisplay(ContentDisplay.RIGHT);
                sender.setFont(new Font(14));
                mld.setGraphic(sender);
                mld.setContentDisplay(ContentDisplay.TOP);
                mld.setFont(new Font(16));
                meldinger.getChildren().add(mld);            
                meldinger.setSpacing(1);
                scroll.setContent(meldinger); 
                scroll.setFitToWidth(true);

                if (id == avsenderId) {
                    mld.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, new CornerRadii(0), Insets.EMPTY)));
                }

                mld.setWrapText(true);  
                mld.setStyle("-fx-background-color: white;");
                mld.setStyle("-fx-border-width: 1px;");
                mld.setStyle("-fx-border-style: solid;");
                mld.setStyle("-fx-border-color: black;");
                mld.setPadding(new Insets(10));
                
            }
            tegneOmråde.setCenter(scroll);
        }
    }
    /**
    * tegnSkriveOmråde - Lager knapper, skirvefelt og legger til ENTER som eventhandler på skrivefelt. 
    */
    public void tegnSkriveOmråde() {
        skriveWrapper.setAlignment(Pos.CENTER);
        skriveWrapper.getChildren().clear();
        skriveWrapper.getChildren().addAll(opprettKnapp, forlatKnapp, skriveFelt, sendKnapp);      
        opprettKnapp.setWrapText(true);
        forlatKnapp.setWrapText(true);
        skriveFelt.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER)  {
                    String text = skriveFelt.getText();
                    sendKnapp.fire();
                    // SEND text til tjener
                    
                    // fjern tekst
                    clearText();
                }
            }
        });   
        
        skriveFelt.setWrapText(true);
        skriveFelt.setPromptText("skriv noe...");
        
        //Hentet fra nett: https://stackoverflow.com/questions/36612545/javafx-textarea-limit
        skriveFelt.setTextFormatter(new TextFormatter<String>(change -> 
            change.getControlNewText().length() <= 500 ? change : null));
        
        tegneOmråde.setBottom(skriveWrapper);
        
    }
   
    public void clearText() {
        skriveFelt.setText(""); 
    }
    
    private static void fjernRom(int romId) throws IOException{
        out.writeObject(7);
        out.writeObject(romId);
        
        romTab.remove(finnRom(romId));
    }
    
    private static Rom finnRom(int id){
        Rom rom = null;
        for(Object o: romTab){
            Rom temp = (Rom)o;
            if(temp.equals(id))
                rom = (Rom)o;
        }
        return rom;
    }
     /**
     * tegnStart - Tegner første side der man velger brukernavn, sjekker om brukernavn er tomt og sender videre
     */
    public void tegnStart(){
        VBox vbox = new VBox();
        Label brukernavn = new Label("Brukernavn:");
        
        vbox.getChildren().addAll(brukernavn, ferdig);
        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(5);
        brukernavn.setGraphic(brukernavnInput);
        brukernavn.setContentDisplay(ContentDisplay.BOTTOM);
        
        brukernavnInput.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER)  {
                    String text = brukernavnInput.getText();
                    if (brukernavnInput.getText().isEmpty()) {
                
                    } else {
                        ferdig.fire();
                        //Send text videre
                        tegnRom();
                        tegnSkriveOmråde();
                    }                 
                }
            }
        });
        
        startOmråde.setCenter(vbox);
    }
     /**
     * sjekkSiste - Åpner en alert hvis bruker er sist i rommet og går videre om bruker trykker OK
     */
    public static void sjekkSiste() throws IOException, ClassNotFoundException {
        
        if(sistePerson()){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            
            alert.setTitle("Forlat rom");
            alert.setHeaderText("Forlater du rommet vil det bli slettet for alltid");
            alert.setResizable(false);
            alert.setContentText("Trykk ok for å fortsette");
            siste = true;
            alert.showAndWait().ifPresent(response -> {
                
                if (response == ButtonType.OK) {
                    skriveFelt.setDisable(true);
                    meldinger.getChildren().clear();
                    
                    videre = true;
                    System.out.println("du vil videre ");
                }
            });

        }
    }
    
    private static boolean sistePerson() throws IOException, ClassNotFoundException{
        System.out.println("Sjekker om du er siste");
        out.writeObject(6);
        out.writeObject(id);
        out.writeObject(iRom);
        siste = false;
        videre = false;
        return (boolean)in.readObject();
    }
    
    /**
    * leggTilBruker - Åpner socket, sender brukernavn til tjener og mottar gitt id
    * @param String brukernavn - brukers valgt brukernavn
    */
    public static void leggTilBruker(String brukernavn) throws IOException, ClassNotFoundException{
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        
        out.writeObject(brukernavn);
        id = (int)in.readObject();
        
    }
    
    private static void leggTilRom(String romnavn) throws IOException, ClassNotFoundException{
        
        out.writeObject(1);
        out.writeObject(id);
        out.writeObject(romnavn);
        
        iRom = (int)in.readObject();
        
        
        fåAlleAktiveRom();
    } 
    /**
    * byttTilRom - Sender rombytte til tjener med id og romId
    * @param Rom rom - romObjektet som holder info
    * 
    */
    private static void byttTilRom(Rom rom) throws IOException{
        out.writeObject(2);
        out.writeObject(id);
        out.writeObject(rom.id);
    }
    /**
    * fåAlleAktiveRom - Får alle aktive rom fra tjener og legger de i en tabell
    */   
    public static void fåAlleAktiveRom() throws IOException, ClassNotFoundException{
        
        out.writeObject(3);
        String[] romnavn = (String[])in.readObject();
        int[] romid = (int[])in.readObject();
        romTab.clear();
        for(int i = 0; i < romnavn.length; i++){
            romTab.add(new Rom(romnavn[i], romid[i]));
        }
    }
    
    public static void fåAlleBrukereIrom(){
        
    }
   
    private static void sendMelding() throws IOException{
        
        out.writeObject(4);
        out.writeObject(skriveFelt.getText());
        out.writeObject(id);
        out.writeObject(iRom);        
    }
    
    private static class Rom{
        String navn;
        int id;
        /**
        *
        */
        public Rom(String navn, int id){
            this.navn = navn;
            this.id = id;
        }
        
        /**
        * toString - returnerer navn på Rom
        * @return - navnet på Rom
        */
        @Override
        public String toString(){
            return navn;
        }
        
        /**
        * equals - sjekker om Objekt o er lik eller noe likt Rom
        * @param - Objekt o - Objektet som sendes inn
        * @return - true eller false om de er like/ulike
        */
        @Override
        public boolean equals(Object o){
            if(o instanceof Rom){
                Rom rom = (Rom)o;
                return this.id == rom.id;
            }
            else if (o instanceof Integer){
                System.out.println("Dette rommet sin id: " + this.id);
                System.out.println("Objektet: " + o);
                System.out.println("Sjekkes mot romId: " + (int)o);
                return this.id == (int)o;
            }
            return false;
        }
    }
    
}