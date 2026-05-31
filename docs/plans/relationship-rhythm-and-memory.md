# CircleKeep Relationship Rhythm Plan

## Summary
CircleKeep'i "kisisel iliski ritmi" uygulamasi olarak buyutecegiz: kullanici global iletisim turleri tanimlayabilecek, kisilere/gruplara ritim verebilecek, kisi bazinda ritim override edebilecek, temaslari notla kaydedebilecek ve dashboard daha akilli hatirlatma bolumleri gosterecek.

"Bulusma planlama / takvim plani" ozelligi eklenmeyecek.

## Key Changes
- Yeni `ContactType` entity: `key`, `label`, `isDefault`, `isActive`, `sortOrder`.
- Varsayilan turler: `CALL` = Arama, `MESSAGE` = Mesaj, `MEETING` = Bulusma.
- `Person` alanlari: `preferredContactTypeKey`, `customFrequencyDays`, `memoryNotes`, `nextConversationHint`, `importantDateLabel`, `importantDateMillis`.
- `InteractionLog` alanı: `note`.
- DB version `5`; migration `4 -> 5` eski veriyi koruyacak.
- `Wave` mevcut grup/dongu rolunu korur; kisi `customFrequencyDays` ile grup ritmini ezebilir.
- Temas turu silme fiziksel delete degil `isActive=false` olarak uygulanir.

## UI Behavior
- Dashboard "Bugun", "Gecikenler", "Yakinda", "Ertelenenler" bolumlerini gosterir.
- Kisi kartinda birincil hizli aksiyon kisinin tercih edilen iletisim turunden gelir.
- Ek hizli aksiyonlar: "Temas ettim", "Notla kaydet", "Yarin hatirlat".
- Kisi detayinda tercih edilen tur, kisi ozel ritmi ve hafiza alanlari duzenlenir.
- Temas gecmisinde tur etiketi, tarih ve opsiyonel not gosterilir.

## Test Plan
- v4 database v5'e cikinca kisi, grup ve eski interaction loglari korunur.
- Yeni kisi alanlari default alir: preferred type `CALL`, custom cadence `NULL`, hafiza alanlari bos.
- Grup ritmi, kisi override ritmi, snooze ve reminder disabled senaryolari test edilir.
- Default/custom/pasif contact type akislari test edilir.
- Backup yeni alanlari export eder ve eski backup'lari guvenli defaultlarla okur.

## Assumptions
- Bulusma planlama veya takvim entegrasyonu bu fazda yok.
- Kisi ayni anda tek gruba bagli kalir.
- Kisinin tek tercih edilen iletisim turu olur; tur basina ayri ritim bu fazda yok.
- Temas turu silme, gecmisi korumak icin pasiflestirme olarak uygulanir.
