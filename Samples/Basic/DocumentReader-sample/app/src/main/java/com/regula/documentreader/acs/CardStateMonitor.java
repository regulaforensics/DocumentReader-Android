/*
 * Copyright (C) 2020 Advanced Card Systems Ltd. All rights reserved.
 *
 * This software is the confidential and proprietary information of Advanced
 * Card Systems Ltd. ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with ACS.
 */

package com.regula.documentreader.acs;

import android.util.Log;

import com.regula.documentreader.api.BuildConfig;

import java.util.HashMap;
import java.util.Map;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

/**
 * The {@code CardStateMonitor} class is a singleton that monitors the card state of the card
 * terminal.
 *
 * @author Godfrey Chung
 * @version 1.0, 6 Feb 2020
 * @since 0.5.1
 */
public final class CardStateMonitor {

    /** The card state is unknown. */
    public static final int CARD_STATE_UNKNOWN = 0;

    /** The card state is absent. */
    public static final int CARD_STATE_ABSENT = 1;

    /** The card state is present. */
    public static final int CARD_STATE_PRESENT = 2;

    /**
     * Interface definition for a callback to be invoked when the card is inserted or removed.
     */
    public interface OnStateChangeListener {

        /**
         * Called when the card is inserted or removed.
         *
         * @param monitor   the card state monitor
         * @param terminal  the card terminal
         * @param prevState the previous state
         * @param currState the current state
         */
        void onStateChange(CardStateMonitor monitor, CardTerminal terminal, int prevState,
                int currState);
    }

    /**
     * The {@code CardDetectionRunnable} class detects a card and reports the state.
     */
    private class CardDetectionRunnable implements Runnable {

        private CardTerminal mTerminal;

        /**
         * Creates an instance of {@code CardDetectionRunnable}l
         *
         * @param terminal the card terminal
         */
        public CardDetectionRunnable(CardTerminal terminal) {

            if (terminal == null) {
                throw new IllegalArgumentException("Terminal must not be null");
            }

            mTerminal = terminal;
        }

        @Override
        public void run() {

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Enter: " + mTerminal.getName());
            }

            int prevState = CARD_STATE_UNKNOWN;
            while (!Thread.interrupted()) {

                /* Get the current state. */
                int currState;
                try {

                    currState = mTerminal.isCardPresent() ? CARD_STATE_PRESENT : CARD_STATE_ABSENT;

                } catch (CardException e) {

                    /* Break if the thread is interrupted. */
                    if (e.getCause() instanceof InterruptedException) {
                        break;
                    }

                    currState = CARD_STATE_UNKNOWN;
                }

                /* Report the current state if the state is changed. */
                if (currState != prevState) {
                    if (mOnStateChangeListener != null) {
                        mOnStateChangeListener.onStateChange(CardStateMonitor.this, mTerminal,
                                prevState, currState);
                    }
                }

                /* Update the previous state. */
                prevState = currState;

                /* Wait for the change. */
                try {

                    if (currState == CARD_STATE_ABSENT) {
                        mTerminal.waitForCardPresent(1000);
                    } else if (currState == CARD_STATE_PRESENT) {
                        mTerminal.waitForCardAbsent(1000);
                    } else {
                        Thread.sleep(500);
                    }

                } catch (CardException e) {

                    /* Break if the thread is interrupted. */
                    if (e.getCause() instanceof InterruptedException) {
                        break;
                    }

                    e.printStackTrace();

                } catch (InterruptedException e) {

                    break;
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Exit: " + mTerminal.getName());
            }
        }
    }

    private static final String TAG = "CardStateMonitor";
    private static final CardStateMonitor INSTANCE = new CardStateMonitor();
    private Map<String, CardTerminal> mTerminals = new HashMap<>();
    private Map<String, Thread> mThreads = new HashMap<>();
    private OnStateChangeListener mOnStateChangeListener;

    /**
     * Creates an instance of {@code CardStateMonitor}.
     */
    private CardStateMonitor() {
    }

    /**
     * Returns the instance of {@code CardStateMonitor}.
     *
     * @return the instance
     */
    public static CardStateMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a callback to be invoked when the card is inserted or removed.
     *
     * @param listener the callback that will run
     */
    public void setOnStateChangeListener(OnStateChangeListener listener) {
        mOnStateChangeListener = listener;
    }

    public void removeOnStateChangeListener() {
        mOnStateChangeListener = null;
    }

    /**
     * Returns {@code true} if the terminal is enabled.
     *
     * @param terminal the card terminal
     * @return {@code true} if the terminal is enabled.
     */
    public boolean isTerminalEnabled(CardTerminal terminal) {

        if (terminal == null) {
            throw new IllegalArgumentException("Terminal must not be null");
        }

        return (mTerminals.containsKey(terminal.getName()));
    }

    /**
     * Adds the terminal to the monitor.
     *
     * @param terminal the terminal
     */
    public void addTerminal(CardTerminal terminal) {
        if (!isTerminalEnabled(terminal)) {

            /* Create a thread for card detection. */
            Thread thread = new Thread(new CardDetectionRunnable(terminal));

            /* Store the thread. */
            mThreads.put(terminal.getName(), thread);

            /* Store the terminal. */
            mTerminals.put(terminal.getName(), terminal);

            /* Start the card detection. */
            thread.start();
        }
    }

    /**
     * Removes the terminal from the monitor.
     *
     * @param terminal the terminal
     */
    public void removeTerminal(CardTerminal terminal) {
        if (isTerminalEnabled(terminal)) {

            /* Terminate the thread. */
            Thread thread = mThreads.get(terminal.getName());
            if (thread != null) {
                thread.interrupt();
            }

            /* Remove the thread. */
            mThreads.remove(terminal.getName());

            /* Remove the terminal. */
            mTerminals.remove(terminal.getName());
        }
    }

    public boolean isContainsTerminal(CardTerminal terminal) {
        return mTerminals.containsKey(terminal.getName());
    }

    /**
     * Resumes the operation.
     */
    public void resume() {
        for (Map.Entry<String, Thread> entry : mThreads.entrySet()) {

            CardTerminal terminal = mTerminals.get(entry.getKey());
            Thread thread = entry.getValue();
            if (thread == null) {

                /* Create a thread for card detection. */
                thread = new Thread(new CardDetectionRunnable(terminal));

                /* Store the thread. */
                entry.setValue(thread);

                /* Start the thread. */
                thread.start();
            }
        }
    }

    /**
     * Pauses the operation.
     */
    public void pause() {
        for (Map.Entry<String, Thread> entry : mThreads.entrySet()) {

            Thread thread = entry.getValue();
            if (thread != null) {

                /* Terminate the thread. */
                thread.interrupt();

                /* Set the thread to null. */
                entry.setValue(null);
            }
        }
    }
}
