package com.brugalibre.visitorparking.domain.model.facility;

import com.brugalibre.visitorparking.app.config.VisitorParkingManagementPersistenceConfig;
import com.brugalibre.visitorparking.domain.model.facility.parking.ParkedCar;
import com.brugalibre.visitorparking.domain.model.facility.parking.ParkingLot;
import com.brugalibre.visitorparking.domain.model.resident.Resident;
import com.brugalibre.visitorparking.domain.model.resident.VisitorParkingCard;
import com.brugalibre.visitorparking.domain.repository.facility.FacilityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(classes = VisitorParkingManagementPersistenceConfig.class)
public class FacilityIntegTest {

   private static final String SG_1 = "SG-1";
   private static final String SG_2 = "SG-2";
   private static final String SG_3 = "SG-3";
   private static final String PARKING_CARD_NR_1 = "12345";
   private static final String PARKING_CARD_NR_2 = "123456";

   @Autowired
   private FacilityRepository facilityRepository;

   @Test
   void testCreateFacilityWithOneParkedCarAndAddTwoParkedCarsAfter() {
      // Given
      String carPlateNo = "SG-12345";
      String userId = "1245";

      // Facility with one user, a ParkingLot with one parked car
      Facility facility = Facility.builder()
              .residents(List.of(Resident.builder()
                      .userId(userId)
                      .visitorParkingCards(List.of(VisitorParkingCard.builder()
                              .parkingCardNr(UUID.randomUUID().toString())
                              .build()))
                      .build()))
              .parkingLot(ParkingLot.builder()
                      .capacity(2)
                      .parkedCars(List.of(ParkedCar.builder()
                              .carPlateNo(carPlateNo)
                              .build()))
                      .build())
              .build();

      // When - add two parked cars
      Facility persistedFacility = facilityRepository.save(facility);
      String parkingLotId = persistedFacility.parkingLot().getId();
      ParkedCarAdded2FacilityResult parkedCarAdded2FacilityResult = persistedFacility.addParkedCar(ParkedCar.builder()
                      .carPlateNo(SG_1)
                      .visitorParkingCard(VisitorParkingCard.builder()
                              .parkingLotId(parkingLotId)
                              .parkingCardNr(PARKING_CARD_NR_1)
                              .isAvailable(true)
                              .build())
                      .build())
              .facility()
              .addParkedCar(ParkedCar.builder()
                      .carPlateNo(SG_2)
                      .visitorParkingCard(VisitorParkingCard.builder()
                              .parkingLotId(parkingLotId)
                              .parkingCardNr(PARKING_CARD_NR_2)
                              .isAvailable(true)
                              .build())
                      .build());
      persistedFacility = facilityRepository.save(parkedCarAdded2FacilityResult.facility());
      boolean isSG1Parked = persistedFacility.isParkedCarAssignedByPlateNr(SG_1);
      boolean isSG2Parked = persistedFacility.isParkedCarAssignedByPlateNr(SG_2);
      boolean isSG3Parked = persistedFacility.isParkedCarAssignedByPlateNr(carPlateNo);

      // Then
      ParkingLot parkingLot = persistedFacility.parkingLot();
      assertThat(isSG1Parked, is(true));
      assertThat(isSG2Parked, is(true));
      assertThat(isSG3Parked, is(false));
      assertThat(parkingLot.capacity(), is(2));
      assertThat(parkingLot.parkedCars().size(), is(3));

      List<Resident> residents = persistedFacility.residents();
      assertThat(residents.size(), is(1));
      assertThat(residents.get(0).userId(), is(userId));
   }

   @Test
   void testCreateFacilityAndTryAddParkedCarWithInvalidVisitorParkingCard() {
      // Given
      String parkingLotId = UUID.randomUUID().toString();

      // Facility with one user, a ParkingLot with one parked car
      Facility facility = Facility.builder()
              .parkingLot(ParkingLot.builder()
                      .capacity(2)
                      .build())
              .build();

      // When - add two parked cars
      Facility persistedFacility = facilityRepository.save(facility);
      ParkedCarAdded2FacilityResult parkedCarAdded2FacilityResult = persistedFacility.addParkedCar(ParkedCar.builder()
              .carPlateNo(SG_1)
              .visitorParkingCard(VisitorParkingCard.builder()
                      .parkingLotId(parkingLotId)
                      .parkingCardNr(PARKING_CARD_NR_1)
                      .isAvailable(true)
                      .build())
              .build());
      persistedFacility = facilityRepository.save(parkedCarAdded2FacilityResult.facility());
      boolean isSG1Parked = persistedFacility.isParkedCarAssignedByPlateNr(SG_1);

      // Then
      ParkingLot parkingLot = persistedFacility.parkingLot();
      assertThat(isSG1Parked, is(false));
      assertThat(parkingLot.capacity(), is(2));
      assertThat(parkingLot.parkedCars().size(), is(0));
   }

   @Test
   void testCreateFacilityWithOneParkedCarAndRemoveItAgain() {
      // Given
      // Facility with one ParkingLot with one parked car
      Facility facility = Facility.builder()
              .parkingLot(ParkingLot.builder()
                      .capacity(2)
                      .parkedCars(List.of(ParkedCar.builder()
                              .carPlateNo(SG_3)
                              .build()))
                      .build())
              .build();

      // When
      Facility persistedFacility = facilityRepository.save(facility);
      persistedFacility = persistedFacility.removeParkedCar(SG_3);
      boolean isSG1Parked = persistedFacility.isParkedCarAssignedByPlateNr(SG_3);

      // Then
      ParkingLot parkingLot = persistedFacility.parkingLot();
      assertThat(parkingLot.capacity(), is(2));
      assertThat(isSG1Parked, is(false));
      assertThat(parkingLot.parkedCars().size(), is(0));
   }
}