package org.usfirst.frc.team2077.drivetrain;

import edu.wpi.first.wpilibj.ADIS16470_IMU;
import org.usfirst.frc.team2077.common.WheelPosition;
import org.usfirst.frc.team2077.common.drivetrain.AbstractChassis;
import org.usfirst.frc.team2077.common.drivetrain.DriveModuleIF;
import org.usfirst.frc.team2077.common.sensor.AngleSensor;
import org.usfirst.frc.team2077.math.SwerveMath;
import org.usfirst.frc.team2077.math.SwerveTargetValues;
import org.usfirst.frc.team2077.subsystem.SwerveModule;
import org.usfirst.frc.team2077.util.Constants.Frame;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

import static org.usfirst.frc.team2077.common.VelocityDirection.*;

public class SwerveChassis extends AbstractChassis<SwerveModule> {

    private final SwerveMath math;
    //ADIS16470_IMU is the class used in the provided swerve code //TODO: check gyro
    private final /*AngleSensor*/ ADIS16470_IMU gyro;

    private static EnumMap<WheelPosition, SwerveModule> buildDriveTrain() {
        EnumMap<WheelPosition, SwerveModule> map = new EnumMap<>(WheelPosition.class);

        for(SwerveModule.MotorPosition p : SwerveModule.MotorPosition.values()){
            map.put(WheelPosition.valueOf(p.name()), new SwerveModule(p));
        }

        return map;
    }

    public SwerveChassis() {
        super(buildDriveTrain());

        gyro = new ADIS16470_IMU();

        math = new SwerveMath(Frame.kWheelBaseLength, Frame.kWheelBaseWidth);

        this.maximumSpeed = this.driveModules.values().stream().map(DriveModuleIF::getMaximumSpeed).min(Comparator.naturalOrder()).orElseThrow();

        //Dear David,
        //  I am sorry that I did not like you clever work around
        //  for finding the max rotation velocity, but it added a
        //  lot of clutter and is likely too confusion to anybody
        //  who did not watch you implement it directly.
        //Sincerely, Hank

        //TODO: confirm that this works
        double circumference = Math.PI * Math.hypot(Frame.kWheelBaseLength, Frame.kWheelBaseWidth);
        //The time it would take for a wheel traveling at maximum speed to travel the distance of the circumference
        double secondsPerRevolution = circumference / this.maximumSpeed;
        double radiansPerSecond = 2 * Math.PI / secondsPerRevolution;

        this.maximumRotation = radiansPerSecond;

        this.minimumSpeed = this.maximumSpeed * 0.1;
    }

    @Override protected void measureVelocity(){
        velocityMeasured = math.velocitiesForTargets(driveModules);
    }

    @Override protected void updateDriveModules() {
        Map<WheelPosition, SwerveTargetValues> wheelTargets = math.targetsForVelocities(
            velocitySet,
            maximumSpeed,
            maximumRotation
        );

        wheelTargets.forEach((key, value) -> {
            SwerveModule module = this.driveModules.get(key);
            double velocity = Math.abs(value.getMagnitude());
            if(velocity > maximumSpeed) velocity = maximumSpeed;
            if(velocity > 0.01) velocity = Math.max(velocity, minimumSpeed);

            module.setAngle(value.getAngle());
            module.setVelocity(velocity);
        });

    }
}