package com.example.housinghub.managers

import android.content.Context
import android.util.Log
import com.example.housinghub.model.Booking
import com.example.housinghub.model.Property
import com.example.housinghub.utils.UserSessionManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class BookingManager(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val userSessionManager = UserSessionManager(context)
    
    companion object {
        private const val TAG = "BookingManager"
        private const val COLLECTION_BOOKINGS = "bookings"
        private const val COLLECTION_TENANTS = "tenants"
        private const val COLLECTION_OWNERS = "owners"
    }
    
    /**
     * Create a new booking record
     */
    suspend fun createBooking(
        property: Property,
        startDate: Date,
        durationMonths: Int,
        numberOfOccupants: Int,
        specialNotes: String
    ): Result<Booking> {
        return try {
            val tenantEmail = userSessionManager.getEmail()
            val tenantName = userSessionManager.getFullName()
            val tenantPhone = userSessionManager.getPhone()
            
            if (tenantEmail.isEmpty()) {
                return Result.failure(Exception("User not logged in"))
            }
            
            // Calculate end date
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            calendar.add(Calendar.MONTH, durationMonths)
            val endDate = calendar.time
            
            // Generate booking ID
            val bookingId = firestore.collection(COLLECTION_BOOKINGS).document().id
            
            // Create booking object
            val booking = Booking(
                id = bookingId,
                tenantEmail = tenantEmail,
                tenantName = tenantName,
                tenantPhone = tenantPhone,
                ownerEmail = property.ownerId,
                propertyId = property.id,
                propertyTitle = property.title,
                propertyLocation = property.location.ifEmpty { property.address },
                propertyPrice = property.price,
                startDate = Timestamp(startDate),
                endDate = Timestamp(endDate),
                durationMonths = durationMonths,
                numberOfOccupants = numberOfOccupants,
                specialNotes = specialNotes,
                totalAmount = property.price * durationMonths,
                securityDeposit = property.price * Booking.SECURITY_DEPOSIT_MONTHS,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            Log.d(TAG, "Creating booking: $bookingId for property: ${property.id}")
            Result.success(booking)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating booking: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save booking to Firestore after successful payment
     */
    suspend fun saveBooking(booking: Booking): Result<Unit> {
        return try {
            Log.d(TAG, "Saving booking: ${booking.id}")
            Log.d(TAG, "Booking details: tenantEmail=${booking.tenantEmail}, ownerEmail=${booking.ownerEmail}, propertyId=${booking.propertyId}")
            
            // Ensure booking has all required data
            if (booking.tenantEmail.isEmpty() || booking.ownerEmail.isEmpty() || booking.propertyId.isEmpty()) {
                throw Exception("Missing required booking data: tenantEmail, ownerEmail, or propertyId")
            }
            
            // Save main booking record
            firestore.collection(COLLECTION_BOOKINGS)
                .document(booking.id)
                .set(booking)
                .await()
            
            Log.d(TAG, "Main booking record saved successfully")
            
            // Save reference in tenant's bookings
            firestore.collection(COLLECTION_TENANTS)
                .document(booking.tenantEmail)
                .collection("bookings")
                .document(booking.id)
                .set(mapOf(
                    "bookingId" to booking.id,
                    "propertyTitle" to booking.propertyTitle,
                    "propertyLocation" to booking.propertyLocation,
                    "startDate" to booking.startDate,
                    "endDate" to booking.endDate,
                    "status" to booking.bookingStatus,
                    "amountPaid" to booking.amountPaid,
                    "createdAt" to booking.createdAt
                ))
                .await()
            
            Log.d(TAG, "Tenant booking reference saved successfully")
            
            // Save reference in owner's property bookings
            firestore.collection("owner_properties")
                .document(booking.ownerEmail)
                .collection("properties")
                .document(booking.propertyId)
                .collection("bookings")
                .document(booking.id)
                .set(mapOf(
                    "bookingId" to booking.id,
                    "tenantName" to booking.tenantName,
                    "tenantEmail" to booking.tenantEmail,
                    "tenantPhone" to booking.tenantPhone,
                    "startDate" to booking.startDate,
                    "endDate" to booking.endDate,
                    "status" to booking.bookingStatus,
                    "amountPaid" to booking.amountPaid,
                    "numberOfOccupants" to booking.numberOfOccupants,
                    "createdAt" to booking.createdAt
                ))
                .await()
            
            Log.d(TAG, "Owner booking reference saved successfully")
            Log.d(TAG, "Booking saved successfully: ${booking.id}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving booking: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update booking after payment success
     */
    suspend fun updateBookingPayment(
        bookingId: String,
        paymentId: String,
        paymentSignature: String,
        razorpayOrderId: String,
        amountPaid: Double
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Updating booking payment: $bookingId")
            
            val updates = mapOf(
                "paymentId" to paymentId,
                "paymentSignature" to paymentSignature,
                "razorpayOrderId" to razorpayOrderId,
                "amountPaid" to amountPaid,
                "bookingStatus" to Booking.STATUS_CONFIRMED,
                "paymentDate" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
            
            firestore.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Booking payment updated successfully: $bookingId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating booking payment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get tenant's bookings
     */
    suspend fun getTenantBookings(): Result<List<Booking>> {
        return try {
            val tenantEmail = userSessionManager.getEmail()
            if (tenantEmail.isEmpty()) {
                return Result.failure(Exception("User not logged in"))
            }
            
            val querySnapshot = firestore.collection(COLLECTION_BOOKINGS)
                .whereEqualTo("tenantEmail", tenantEmail)
                .get()
                .await()
            
            val bookings = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.apply {
                    id = doc.id
                }
            }
            
            Log.d(TAG, "Retrieved ${bookings.size} bookings for tenant: $tenantEmail")
            Result.success(bookings)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving tenant bookings: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get owner's property bookings
     */
    suspend fun getOwnerBookings(): Result<List<Booking>> {
        return try {
            val ownerEmail = userSessionManager.getEmail()
            if (ownerEmail.isEmpty()) {
                return Result.failure(Exception("User not logged in"))
            }
            
            val querySnapshot = firestore.collection(COLLECTION_BOOKINGS)
                .whereEqualTo("ownerEmail", ownerEmail)
                .get()
                .await()
            
            val bookings = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.apply {
                    id = doc.id
                }
            }
            
            Log.d(TAG, "Retrieved ${bookings.size} bookings for owner: $ownerEmail")
            Result.success(bookings)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving owner bookings: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancel a booking
     */
    suspend fun cancelBooking(bookingId: String, reason: String): Result<Unit> {
        return try {
            Log.d(TAG, "Cancelling booking: $bookingId")
            
            // First get the booking details to check if property needs to be made available again
            val bookingDoc = firestore.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .get()
                .await()
            
            val booking = bookingDoc.toObject(Booking::class.java)
                ?: return Result.failure(Exception("Booking not found"))
            
            val updates = mapOf(
                "bookingStatus" to Booking.STATUS_CANCELLED,
                "cancellationReason" to reason,
                "updatedAt" to Timestamp.now()
            )
            
            firestore.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .update(updates)
                .await()
            
            // If booking was approved, transfer property back to Available
            if (booking.bookingStatus == Booking.STATUS_APPROVED) {
                transferPropertyToAvailable(booking.ownerEmail, booking.propertyId)
                Log.d(TAG, "Property transferred back to Available due to cancellation")
            }
            
            Log.d(TAG, "Booking cancelled successfully: $bookingId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling booking: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Approve a booking by owner
     */
    suspend fun approveBooking(bookingId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Approving booking: $bookingId")
            
            // First get the booking details to access property information
            val bookingDoc = firestore.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .get()
                .await()
            
            val booking = bookingDoc.toObject(Booking::class.java)
                ?: return Result.failure(Exception("Booking not found"))
            
            // Update booking status
            val bookingUpdates = mapOf(
                "bookingStatus" to Booking.STATUS_APPROVED,
                "approvedAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
            
            firestore.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .update(bookingUpdates)
                .await()
            
            // Transfer property from Available to Unavailable collection
            transferPropertyToUnavailable(booking.ownerEmail, booking.propertyId)
            
            Log.d(TAG, "Booking approved successfully: $bookingId and property transferred to unavailable")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error approving booking: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Transfer property from Available to Unavailable collection
     */
    private suspend fun transferPropertyToUnavailable(ownerEmail: String, propertyId: String) {
        try {
            Log.d(TAG, "Transferring property $propertyId from Available to Unavailable for owner $ownerEmail")
            
            // References to source and destination
            val sourceRef = firestore.collection("Properties")
                .document(ownerEmail)
                .collection("Available")
                .document(propertyId)
            
            val destRef = firestore.collection("Properties")
                .document(ownerEmail)
                .collection("Unavailable")
                .document(propertyId)
            
            // Use transaction to ensure atomicity
            firestore.runTransaction { transaction ->
                // Get property from Available collection
                val propertyDoc = transaction.get(sourceRef)
                if (!propertyDoc.exists()) {
                    throw Exception("Property not found in Available collection")
                }
                
                // Get property data and update availability status
                val propertyData = propertyDoc.data?.toMutableMap() 
                    ?: throw Exception("Invalid property data")
                
                propertyData["isAvailable"] = false
                propertyData["updatedAt"] = Timestamp.now()
                
                // Move to Unavailable collection
                transaction.set(destRef, propertyData)
                
                // Remove from Available collection
                transaction.delete(sourceRef)
                
                Log.d(TAG, "Property $propertyId successfully transferred to Unavailable collection")
            }.await()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error transferring property to unavailable: ${e.message}", e)
            // Don't fail the booking approval if property transfer fails
            // Just log the error - the booking is still approved
        }
    }
    
    /**
     * Transfer property from Unavailable back to Available collection
     */
    private suspend fun transferPropertyToAvailable(ownerEmail: String, propertyId: String) {
        try {
            Log.d(TAG, "Transferring property $propertyId from Unavailable to Available for owner $ownerEmail")
            
            // References to source and destination
            val sourceRef = firestore.collection("Properties")
                .document(ownerEmail)
                .collection("Unavailable")
                .document(propertyId)
            
            val destRef = firestore.collection("Properties")
                .document(ownerEmail)
                .collection("Available")
                .document(propertyId)
            
            // Use transaction to ensure atomicity
            firestore.runTransaction { transaction ->
                // Get property from Unavailable collection
                val propertyDoc = transaction.get(sourceRef)
                if (!propertyDoc.exists()) {
                    throw Exception("Property not found in Unavailable collection")
                }
                
                // Get property data and update availability status
                val propertyData = propertyDoc.data?.toMutableMap() 
                    ?: throw Exception("Invalid property data")
                
                propertyData["isAvailable"] = true
                propertyData["updatedAt"] = Timestamp.now()
                
                // Move to Available collection
                transaction.set(destRef, propertyData)
                
                // Remove from Unavailable collection
                transaction.delete(sourceRef)
                
                Log.d(TAG, "Property $propertyId successfully transferred to Available collection")
            }.await()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error transferring property to available: ${e.message}", e)
            // Don't fail the booking cancellation if property transfer fails
        }
    }
    
    /**
     * Reject a booking by owner
     */
    suspend fun rejectBooking(bookingId: String, reason: String): Result<Unit> {
        return try {
            Log.d(TAG, "Rejecting booking: $bookingId")
            
            val updates = mapOf(
                "bookingStatus" to Booking.STATUS_REJECTED,
                "rejectionReason" to reason,
                "rejectedAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
            
            firestore.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Booking rejected successfully: $bookingId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting booking: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get pending bookings for owner (bookings that need approval)
     */
    suspend fun getOwnerPendingBookings(): Result<List<Booking>> {
        return try {
            val ownerEmail = userSessionManager.getEmail()
            if (ownerEmail.isEmpty()) {
                return Result.failure(Exception("User not logged in"))
            }
            
            val querySnapshot = firestore.collection(COLLECTION_BOOKINGS)
                .whereEqualTo("ownerEmail", ownerEmail)
                .whereEqualTo("bookingStatus", Booking.STATUS_CONFIRMED)
                .get()
                .await()
            
            val bookings = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.apply {
                    id = doc.id
                }
            }
            
            Log.d(TAG, "Retrieved ${bookings.size} pending bookings for owner: $ownerEmail")
            Result.success(bookings)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving pending bookings: ${e.message}", e)
            Result.failure(e)
        }
    }
}