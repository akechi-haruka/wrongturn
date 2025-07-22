package eu.haruka.wrongturn.objects;

import eu.haruka.wrongturn.TurnLogBack;

public class NullLog implements TurnLogBack {
    @Override
    public void debug(String str) {
    }

    @Override
    public void info(String str) {

    }

    @Override
    public void warning(String str) {

    }

    @Override
    public void error(String str) {

    }

    @Override
    public void exception(Throwable tr, String str) {

    }
}
