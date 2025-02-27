package frc.robot.subsystems;

import frc.robot.SwerveModule;
import frc.robot.Constants;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import com.ctre.phoenix.sensors.Pigeon2;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Swerve extends SubsystemBase {
    public SwerveDriveOdometry swerveOdometry;
    public SwerveModule[] mSwerveMods;
    public Pigeon2 gyro;
    double correctedRotation;
    double limeTranslationX;
    double yaw;
    int rotate;
    double tx;
    int slowDrive;
    double slowDriveDivide;
    boolean slowDriveBool;
    Limelight limelight;

    public Swerve() {
        gyro = new Pigeon2(Constants.Swerve.pigeonID);
        gyro.configFactoryDefault();
        zeroGyro();
        yaw = getYaw().getDegrees();
        correctedRotation = 0;
        rotate = 0;
        slowDrive = 0;
        slowDriveBool = false;
        mSwerveMods = new SwerveModule[] {
                new SwerveModule(0, Constants.Swerve.Mod0.constants),
                new SwerveModule(1, Constants.Swerve.Mod1.constants),
                new SwerveModule(2, Constants.Swerve.Mod2.constants),
                new SwerveModule(3, Constants.Swerve.Mod3.constants)
        };

        /*
         * By pausing init for a second before setting module offsets, we avoid a bug
         * with inverting motors.
         * See https://github.com/Team364/BaseFalconSwerve/issues/8 for more info.
         */
        Timer.delay(1.0);
        resetModulesToAbsolute();

        swerveOdometry = new SwerveDriveOdometry(Constants.Swerve.swerveKinematics, getYaw(), getModulePositions());
    }

    /** Rotate right */
    public void rotateRight() {
        yaw = getYaw().getDegrees();
        correctedRotation = -10;
        rotate = -1;
        SmartDashboard.putBoolean("rotateRight", true);
    }

    /** Rotate right */
    public void rotateLeft() {
        yaw = getYaw().getDegrees();
        correctedRotation = 10;
        rotate = 1;
        SmartDashboard.putBoolean("rotateRight", false);
    }

    public void slowDrive() {
        if (slowDrive == 1) {
            slowDrive = 0;
        } else {
            slowDrive = 1;
        }
    }

    public void drive(Translation2d translation, double rotation, boolean fieldRelative, boolean isOpenLoop) {
        rotation += correctedRotation;
        SwerveModuleState[] swerveModuleStates = Constants.Swerve.swerveKinematics.toSwerveModuleStates(
                fieldRelative ? ChassisSpeeds.fromFieldRelativeSpeeds(
                        translation.getX()/slowDriveDivide,
                        translation.getY()/slowDriveDivide,
                        rotation/slowDriveDivide,
                        getYaw())
                        : new ChassisSpeeds(
                                translation.getX(),
                                translation.getY(),
                                rotation));
        SwerveDriveKinematics.desaturateWheelSpeeds(swerveModuleStates, Constants.Swerve.maxSpeed);
        if (rotate == 1) {
            if (getYaw().getDegrees() > yaw + 15) {
                correctedRotation = 0;
                rotate = 0;
            }
        }
        if (rotate == -1) {
            if (getYaw().getDegrees() < yaw - 15) {
                correctedRotation = 0;
                rotate = 0;
            }
        }
        if (slowDrive == 1) {
            slowDriveDivide = 1;
            slowDriveBool = false;
            // limelight.limePower(true);
            // limelight.table.getEntry("camMode").setNumber(0);
            // tx = limelight.tx;
            // if (tx > 1) {
            //     limeTranslationX -= 0.2;
            // } else if (tx < -1) {
            //     limeTranslationX += 0.2;
            // } else if (tx < 1 && tx > -1) {
            //     limeTranslationX = 0;
            // }
        } else {
            slowDriveDivide = 0.5;
            slowDriveBool = true;
            // limelight.limePower(false);
            // limelight.table.getEntry("camMode").setNumber(1);
            // limeTranslationX = translation.getX();
        }
        for (SwerveModule mod : mSwerveMods) {
            mod.setDesiredState(swerveModuleStates[mod.moduleNumber], isOpenLoop);
        }
        SmartDashboard.putBoolean("slowDrive", slowDriveBool);
    }

    /* Used by SwerveControllerCommand in Auto */
    public void setModuleStates(SwerveModuleState[] desiredStates) {
        SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, Constants.Swerve.maxSpeed);

        for (SwerveModule mod : mSwerveMods) {
            mod.setDesiredState(desiredStates[mod.moduleNumber], false);
        }
    }

    public void setX() {
        mSwerveMods[0].setDesiredState(new SwerveModuleState(0.1, Rotation2d.fromDegrees(45)), false);
        mSwerveMods[1].setDesiredState(new SwerveModuleState(0.1, Rotation2d.fromDegrees(315)), false);
        mSwerveMods[2].setDesiredState(new SwerveModuleState(0.1, Rotation2d.fromDegrees(315)), false);
        mSwerveMods[3].setDesiredState(new SwerveModuleState(0.1, Rotation2d.fromDegrees(45)), false);
        // m_frontLeft.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(45)));
        // m_frontRight.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(-45)));
        // m_rearLeft.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(-45)));
        // m_rearRight.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(45)));
      }

    public Pose2d getPose() {
        return swerveOdometry.getPoseMeters();
    }

    public void resetOdometry(Pose2d pose) {
        swerveOdometry.resetPosition(getYaw(), getModulePositions(), pose);
    }

    public SwerveModuleState[] getModuleStates() {
        SwerveModuleState[] states = new SwerveModuleState[4];
        for (SwerveModule mod : mSwerveMods) {
            states[mod.moduleNumber] = mod.getState();
        }
        return states;
    }

    public SwerveModulePosition[] getModulePositions() {
        SwerveModulePosition[] positions = new SwerveModulePosition[4];
        for (SwerveModule mod : mSwerveMods) {
            positions[mod.moduleNumber] = mod.getPosition();
        }
        return positions;
    }

    public void zeroGyro() {
        gyro.setYaw(0);
    }
    public double getRoll() {
        return gyro.getRoll();
    }
    public Rotation2d getYaw() {
        return (Constants.Swerve.invertGyro) ? Rotation2d.fromDegrees(360 - gyro.getYaw())
                : Rotation2d.fromDegrees(gyro.getYaw());
    }

    public void resetModulesToAbsolute() {
        for (SwerveModule mod : mSwerveMods) {
            mod.resetToAbsolute();
        }
    }

    @Override
    public void periodic() {
        swerveOdometry.update(getYaw(), getModulePositions());

        for (SwerveModule mod : mSwerveMods) {
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " Cancoder", mod.getCanCoder().getDegrees());
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " Integrated", mod.getPosition().angle.getDegrees());
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " Velocity", mod.getState().speedMetersPerSecond);
        }
    }
}