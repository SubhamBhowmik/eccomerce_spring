controller
   ↓
service
   ↓
repository  <-- layer of the project

this will be scaled




 OTP endpoints here:
POST /api/auth/send-otp    ← send OTP to email
POST /api/auth/verify-otp  ← verify OTP + get token
