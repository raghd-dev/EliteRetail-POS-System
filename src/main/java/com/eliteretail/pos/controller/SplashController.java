package com.eliteretail.pos.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;


public class SplashController {


    @FXML
    private StackPane root;


    @FXML
    private ImageView logoWrap;


    @FXML
    private VBox titleGroup;


    @FXML
    private Label subtitleLabel;


    @FXML
    private Button getStartedButton;


    private Runnable onGetStarted;



    @FXML
    private void initialize() {

        hideNode(logoWrap);
        hideNode(titleGroup);
        hideNode(subtitleLabel);
        hideNode(getStartedButton);

    }



    private void hideNode(javafx.scene.Node node){

        node.setOpacity(0);
        node.setTranslateY(30);

    }




    public void play(){


        ParallelTransition animation =
                new ParallelTransition(

                        show(logoWrap,0),

                        show(titleGroup,300),

                        show(subtitleLabel,600),

                        show(getStartedButton,900)

                );


        animation.play();

    }




    private FadeTransition show(
            javafx.scene.Node node,
            int delay
    ){


        FadeTransition fade =
                new FadeTransition(
                        Duration.millis(600),
                        node
                );


        fade.setFromValue(0);

        fade.setToValue(1);


        fade.setInterpolator(
                Interpolator.EASE_OUT
        );


        TranslateTransition move =
                new TranslateTransition(
                        Duration.millis(600),
                        node
                );


        move.setFromY(30);

        move.setToY(0);


        ParallelTransition result =
                new ParallelTransition(
                        fade,
                        move
                );


        result.setDelay(
                Duration.millis(delay)
        );


        return fade;

    }




    public void setOnGetStarted(Runnable action){

        this.onGetStarted = action;

    }




    @FXML
    private void handleGetStarted(){


        if(onGetStarted != null){

            onGetStarted.run();

        }

    }

}