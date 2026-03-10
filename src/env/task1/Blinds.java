package task1;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.OPERATION;
import cartago.LINK;
import cartago.ObsProperty;

public class Blinds extends Artifact {

    ArtifactId illuminanceSensorId;

    public void init(){
        defineObsProperty("blinds", "lowered");
    }

    @OPERATION
    public void raiseBlinds() {
        ObsProperty prop = getObsProperty("blinds");

        if ("lowered".equals(prop.stringValue())) {
            prop.updateValue("raised");

            try {
                ArtifactId illuminanceSensorId = lookupArtifact("illuminance_sensor");
                execLinkedOp(illuminanceSensorId,"increase", 400);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    @OPERATION
    public void lowerBlinds() {
        ObsProperty prop = getObsProperty("blinds");

        if ("raised".equals(prop.stringValue())) {
            prop.updateValue("lowered");

            try {
                ArtifactId illuminanceSensorId = lookupArtifact("illuminance_sensor");
                execLinkedOp(illuminanceSensorId,"decrease", 400);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    @LINK
    public void winterIsComingWithBlinds() {
        ObsProperty prop = getObsProperty("blinds");

        if ("raised".equals(prop.stringValue())) {

            try {
                ArtifactId illuminanceSensorId = lookupArtifact("illuminance_sensor");
                execLinkedOp(illuminanceSensorId,"decrease", 400);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
}