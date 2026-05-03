package ink.astrius.create_avionics.api.simulated;

public interface AnalogTransmissionExt {
    int getSignal();
    boolean isOversaturated();
    boolean isExternallyControlled();
    void setExternallyControlled(boolean externallyControlled);
    void applySignal(int newSignal);
}
