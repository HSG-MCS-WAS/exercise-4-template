package task1;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.OPERATION;
import cartago.ObsProperty;

public class LightBulb extends Artifact {

    public void init(){
        defineObsProperty("lights", "off");
    }

    @OPERATION
    public void turnOnLights() {
        ObsProperty prop = getObsProperty("lights");

        if ("off".equals(prop.stringValue())) {
            prop.updateValue("on");

            try {
                ArtifactId illuminanceSensorId = lookupArtifact("illuminance_sensor");
                execLinkedOp(illuminanceSensorId,"increase", 400);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    @OPERATION
    public void turnOffLights() {
        ObsProperty prop = getObsProperty("lights");

        if ("on".equals(prop.stringValue())) {
            prop.updateValue("off");

            try {
                ArtifactId illuminanceSensorId = lookupArtifact("illuminance_sensor");
                execLinkedOp(illuminanceSensorId,"decrease", 400);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
}