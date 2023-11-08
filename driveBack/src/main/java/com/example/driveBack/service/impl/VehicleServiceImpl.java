package com.example.driveBack.service.impl;

import com.example.driveBack.dto.RideDTO;
import com.example.driveBack.dto.VehiclePreview;
import com.example.driveBack.dto.VehiclePreviewI;
import com.example.driveBack.model.Driver;
import com.example.driveBack.model.Position;
import com.example.driveBack.model.Vehicle;
import com.example.driveBack.model.VehicleState;
import com.example.driveBack.repo.VehicleRepository;
import com.example.driveBack.service.RideService;
import com.example.driveBack.service.VehicleService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    VehicleRepository vehicleRepository;
    @Autowired
    RideService rideService;

    @Override
    public List<VehiclePreview> getNearest(Position currentPosition, Position newPosition) {
        List<VehiclePreviewI> nearestVehicles = vehicleRepository.findNearestVehicles(currentPosition.getLatitude(), currentPosition.getLongitude());

        List<VehiclePreview> vp = nearestVehicles.stream()
                .map(VehiclePreview::new)
                .collect(Collectors.toList());

        vp.forEach(v -> {
            v.setTotalDistance(DistanceCalculator.calculateDistance(currentPosition, newPosition));
            v.setTotalPrice();
        });

        return vp;

    }

    @Override
    public boolean bookVehicle(RideDTO rideDTO) {
        boolean notRejected = simulateTaxiRejectChance();
        if(notRejected) {
            bookRealVehicle(rideDTO.getRideId());
            rideService.makeRide(rideDTO);
        }
        return notRejected;
    }

    @Override
    public Vehicle getVehicle(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new EntityExistsException("Vehicle does not exist."));
    }

    private void bookRealVehicle(Long id) {
        Vehicle vehicle = getVehicle(id);
        vehicle.setState(VehicleState.BOOKED);
        vehicleRepository.save(vehicle);
    }

    private boolean simulateTaxiRejectChance() {
        return Math.random() >= 0.25;
    }
}
