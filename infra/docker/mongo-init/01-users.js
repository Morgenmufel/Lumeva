// Создаёт пользователя для read-модели profile-service с правами только на свою базу.
// Выполняется один раз — при первом старте контейнера с пустым томом.
db = db.getSiblingDB('lumeva_profile_read');

db.createUser({
  user: 'profile',
  pwd: process.env.MONGO_PROFILE_PASSWORD,
  roles: [{ role: 'readWrite', db: 'lumeva_profile_read' }]
});
