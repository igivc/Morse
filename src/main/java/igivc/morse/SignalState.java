package igivc.morse;

enum SignalLevel {Low, High}

class SignalState {
    public int durationInSamples = 0;
    public SignalLevel  signalLevel = SignalLevel.Low;

    public SignalState cloneState() {
        final SignalState s = new SignalState();
        s.durationInSamples = this.durationInSamples;
        s.signalLevel = this.signalLevel;
        return s;
    }

    @Override
    public String toString() {
        return "signalLevel=" + signalLevel + ", durationInSamples=" + durationInSamples;
    }
}
