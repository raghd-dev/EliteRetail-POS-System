package com.eliteretail.pos.utils;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;

/** Lightweight UI-only transitions and brand decoration. */
public final class UiEffects {
    private static final Duration ENTER_DURATION = Duration.millis(180);
    private static final Duration HOVER_DURATION = Duration.millis(85);

    private UiEffects() {}

    public static void prepare(Parent root) {
        installInteractions(root);
        root.setOpacity(0.0);
        root.setTranslateY(6.0);
        Platform.runLater(() -> {
            FadeTransition fade = new FadeTransition(ENTER_DURATION, root);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            TranslateTransition move = new TranslateTransition(ENTER_DURATION, root);
            move.setFromY(6.0);
            move.setToY(0.0);
            new ParallelTransition(fade, move).play();
        });
    }

    public static void applyStageBranding(Stage stage) {
        if (stage == null || !stage.getIcons().isEmpty()) {
            return;
        }
        try (InputStream stream = UiEffects.class.getResourceAsStream("/images/app-icon-64.png")) {
            if (stream != null) {
                stage.getIcons().add(new Image(stream));
            }
        } catch (Exception ignored) {
            // Branding must never prevent a functional window from opening.
        }
    }

    public static void styleDialog(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }
        var css = UiEffects.class.getResource("/css/styles.css");
        if (css != null && !dialog.getDialogPane().getStylesheets().contains(css.toExternalForm())) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        for (ButtonType type : dialog.getDialogPane().getButtonTypes()) {
            Node button = dialog.getDialogPane().lookupButton(type);
            if (button != null) {
                ButtonBar.ButtonData data = type.getButtonData();
                if (data == ButtonBar.ButtonData.OK_DONE || data == ButtonBar.ButtonData.YES) {
                    if (!button.getStyleClass().contains("primary-button")) {
                        button.getStyleClass().add("primary-button");
                    }
                } else if (data == ButtonBar.ButtonData.CANCEL_CLOSE || data == ButtonBar.ButtonData.NO) {
                    if (!button.getStyleClass().contains("secondary-button")) {
                        button.getStyleClass().add("secondary-button");
                    }
                }
            }
        }
        installInteractions(dialog.getDialogPane());
        dialog.setOnShown(event -> {
            if (dialog.getDialogPane().getScene() != null && dialog.getDialogPane().getScene().getWindow() instanceof Stage stage) {
                applyStageBranding(stage);
            }
        });
    }

    private static void installInteractions(Node node) {
        if (node instanceof ButtonBase button) {
            button.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED, event -> animateScale(button, 1.018));
            button.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_EXITED, event -> animateScale(button, 1.0));
            button.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> animateScale(button, 0.985));
            button.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> animateScale(button, 1.0));
        }
        if (node.getStyleClass().contains("interactive-card")) {
            node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED, event -> animateTranslate(node, -2.0));
            node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_EXITED, event -> animateTranslate(node, 0.0));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                installInteractions(child);
            }
        }
    }

    private static void animateScale(Node node, double scale) {
        if (node.isDisabled()) {
            return;
        }
        ScaleTransition transition = new ScaleTransition(HOVER_DURATION, node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    private static void animateTranslate(Node node, double y) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(110), node);
        transition.setToY(y);
        transition.play();
    }
}
