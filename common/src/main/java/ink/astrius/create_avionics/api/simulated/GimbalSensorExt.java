package ink.astrius.create_avionics.api.simulated;

import org.joml.Vector3dc;

public interface GimbalSensorExt {
    Vector3dc getAngularVelocityBody();
    Vector3dc getGravityBody();
    Vector3dc getLinearAccelerationBody();
}
