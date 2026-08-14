package com.eliteretail.pos.app;

import com.eliteretail.pos.controller.SplashController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EliteRetailApplication extends Application {

    private static final Logger LOGGER =
            Logger.getLogger(EliteRetailApplication.class.getName());


    // Main application size
    private static final double APP_WIDTH = 1100;
    private static final double APP_HEIGHT = 720;


    @Override
    public void start(Stage primaryStage) {

        showSplash(primaryStage);

    }


    private void showSplash(Stage primaryStage) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            Objects.requireNonNull(
                                    getClass().getResource(
                                            "/fxml/splash.fxml"
                                    )
                            )
                    );


            Parent splashRoot =
                    loader.load();


            SplashController controller =
                    loader.getController();



            Stage splashStage =
                    new Stage();


            splashStage.initStyle(
                    StageStyle.UNDECORATED
            );


            Scene splashScene =
                    new Scene(
                            splashRoot,
                            APP_WIDTH,
                            APP_HEIGHT,
                            Color.TRANSPARENT
                    );


            splashScene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource(
                                    "/css/styles.css"
                            )
                    ).toExternalForm()
            );


            splashStage.setScene(
                    splashScene
            );


            splashStage.centerOnScreen();


            controller.setOnGetStarted(() -> {

                splashStage.close();

                showLogin(primaryStage);

            });


            splashStage.show();


            controller.play();



        } catch(Exception e){

            LOGGER.log(
                    Level.SEVERE,
                    "Failed to load splash screen",
                    e
            );

        }

    }



    private void showLogin(Stage primaryStage) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            Objects.requireNonNull(
                                    getClass().getResource(
                                            "/fxml/login.fxml"
                                    )
                            )
                    );


            Parent root =
                    loader.load();



            Scene scene =
                    new Scene(
                            root,
                            APP_WIDTH,
                            APP_HEIGHT
                    );


            primaryStage.setScene(scene);


            primaryStage.setTitle(
                    "EliteRetail POS"
            );


            primaryStage.show();


            primaryStage.centerOnScreen();



        } catch(IOException e){

            LOGGER.log(
                    Level.SEVERE,
                    "Failed loading login screen",
                    e
            );

        }

    }



    public static void main(String[] args){

        launch(args);

    }

}