package task1;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.LINK;
import cartago.ObsProperty;

public class IlluminanceSensor extends Artifact {
    
    public void init(){
        defineObsProperty("current_illuminance", 0);
    }

    @LINK
    public void increase(int increment) {
        ObsProperty prop = getObsProperty("current_illuminance");

        for (int i = 0; i<increment/100; i++) {
            await_time(1000);
            
            if (prop.intValue()+100 <= 1000) {
                prop.updateValue(prop.intValue()+100);
            } else {
                prop.updateValue(1000);
            }
        }
        if (prop.intValue() == 400) {

            try {
                ArtifactId weatherStationId = lookupArtifact("weather_station");
                execLinkedOp(weatherStationId,"winterIsComing");
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    @LINK
    public void decrease(Integer increment) {
        ObsProperty prop = getObsProperty("current_illuminance");

        for (int i = 0; i<increment/100; i++) {
            await_time(1000);

            if (prop.intValue()-100 >= 0) {
                prop.updateValue(prop.intValue()-100);
            } else if (prop.intValue()!=0) {
                prop.updateValue(0);
            }
        }
    }

}
