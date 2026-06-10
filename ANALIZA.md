# Analiza obserwowanej dynamiki i dalsze prace

Dokument zbiera **interpretację** zachowań, które widać w symulacji, oraz **planowane usprawnienia**.
Materiał wprost zasila sekcje raportu: **f** (analiza i interpretacja wyników) oraz **g**
(wnioski i dalsze kierunki). Powiązane: `MODEL.md`, `DOKUMENTACJA.md`.

---

## 1. Interpretacja obserwowanej dynamiki

### 1.1. Kolejność wymierania i „dożywający" L5
Przy parametrach domyślnych poziomy wymierają w kolejności: **L4 → L3 → L1 (trawa) → L5 → L2**,
a drapieżnik szczytowy (L5) bardzo długo dożywa jako 1–2 sztuki. To wygląda paradoksalnie, ale wynika z reguł:

- **Poziomy środkowe (L3, L4) są ściskane z dwóch stron:** muszą aktywnie polować na coraz
  rzadszą ofiarę *i* same są zjadane. To najbardziej obciążone i najbardziej ryzykowne poziomy → giną pierwsze.
- **L5 dożywa nie dlatego, że prosperuje, lecz dlatego, że jest odizolowanym „zbiornikiem energii":**
  nikt go nie zjada (brak L6), ma duże rezerwy (`initialEnergy` 400, `maxEnergy` 800) i niski
  metabolizm (0.7). Gdy jego ofiara (L4) znika (~krok 250), L5 jedynie powoli się wypala:
  400 / 0.7 ≈ **570 kroków** biernego głodzenia. To „ostatni żar", nie żywa populacja.
- **L1 → L2 na końcu:** po utracie drapieżników (L3) roślinożercy (L2) robią boom i **przeżerają
  trawę do zera**; potem same dogorywają z resztek rezerw.

**Wniosek dla raportu:** im wyższy i bardziej „zapracowany" poziom, tym szybciej ginie; duży,
bierny, niezjadany zapas energii (L5) gaśnie ostatni. To bezpośrednia ilustracja kruchości
wierzchołka piramidy energii.

### 1.2. Trawa jako stan absorbujący (czemu nie odrasta „z niczego")
W obecnym modelu **producent rozmnaża się wyłącznie z istniejącego producenta**; nie ma tłowego
pojawiania się trawy w pustych miejscach. Zatem **0 trawy = stan bez powrotu** — i to przesądza
o całkowitym kolapsie po przeżarciu.

To świadome uproszczenie. Warto odnotować, że nasz dokument ze Stepu 3 dopuszczał wariant
„spawn continuously at a predetermined **background rate**" — którego nie zaimplementowaliśmy.
W przyrodzie trawa wraca z banku nasion / nasion nawianych z zewnątrz (tak działa model NetLogo
Wolf–Sheep, gdzie trawa odrasta per-komórka niezależnie od sąsiadów).

### 1.3. Zastygłe klastry (czemu osobniki nie eksplorują)
Skupiska, które „dreptają w miejscu", to emergentny skutek trzech reguł działających razem:
1. **Kohezja** aktywnie ściąga rówieśników do środka masy i trzyma ich razem; separacja działa
   tylko na krótkim dystansie (18 px) → ciasny kłębek.
2. **Błądzenie jest bez pamięci** — losowy skok ±`moveSpeed` w nowym losowym kierunku co krok.
   To ruch dyfuzyjny (przemieszczenie ~√t), czyli praktycznie stanie w miejscu; brak *kierunkowego*
   przeszukiwania.
3. **Ograniczona percepcja** — ofiary poza promieniem percepcji są „niewidzialne", a powolne
   błądzenie rzadko tam zaniesie.
W klastrze bez widocznej ofiary steruje kohezja + separacja (znoszą się) → agent oscyluje wokół
stada zamiast ruszyć dalej. Brakuje „popędu eksploracji".

> Punkty 1.2 i 1.3 napędzają 1.1: trawa, która nie wraca, oraz drapieżniki uwięzione w wyjedzonej
> łacie = szybsze głodzenie górnych poziomów i nieodwracalny kolaps.

---

## 2. Wyniki przemiatania parametrów (eksperymenty M4)

Bezgłowy runner (`./gradlew :composeApp:sweep`) uruchamia symulację bez GUI dla siatki parametrów,
uśrednia po wielu ziarnach i zapisuje CSV do `./results`. Poniżej wyniki dla świata 800×600,
1500 kroków, 15 ziaren. **Metryka:** `persist` = średni ostatni krok, w którym poziom jeszcze
istniał (im większy, tym dłużej poziom przetrwał).

### 2.1. Przeżywalność a wydajność transferu energii (reguła 10%) — `exp1_yield.csv`
| yield | L1 persist | L3 persist | L4 persist | L5 persist |
|---|---|---|---|---|
| 0.05 | 1013 | 327 | 377 | **610** |
| 0.10 | 1072 | 386 | 373 | **671** |
| 0.20 | 625 | 569 | 406 | **755** |
| 0.30 | 524 | 601 | 494 | **855** |

- **Kluczowy wynik:** trwałość drapieżnika szczytowego **rośnie monotonicznie** z wydajnością
  transferu (610 → 855 kroków, gdy yield rośnie 5% → 30%). Im mniej energii ginie na każdym
  szczeblu, tym dłużej utrzymuje się wierzchołek piramidy — bezpośrednie potwierdzenie wpływu reguły 10%.
- **Niuans:** trwałość trawy (L1) **maleje** ze wzrostem yield (1013 → 524). Wydajniejsze
  drapieżniki utrzymują więcej roślinożerców → silniejsza presja wypasu → trawa zostaje
  przeżarta wcześniej. Czyli efektywniejszy transfer pomaga górze, ale obciąża podstawę.
- **Uwaga metodologiczna:** udział „przeżywających do końca" (kolumny `*_surv`) wynosi 0 dla
  wszystkich konsumentów przy każdym yield — bo bez odradzania trawy (pkt 1.2) **kolaps jest
  nieunikniony** do kroku 1500. Dlatego sensowną miarą jest *czas trwania*, a nie binarne
  przeżycie. To wprost motywuje usprawnienie `backgroundSpawn` (pkt 2 → „Planowane usprawnienia").

### 2.2. Wpływ zachowań (stadność × żerowanie gradientowe) — `exp2_behaviours.csv`
| herding | gradient | L4 persist | L5 persist |
|---|---|---|---|
| nie | nie | 325 | 671 |
| nie | tak | 331 | 666 |
| **tak** | nie | **416** | 660 |
| tak | tak | 387 | 644 |

- **Stadność wyraźnie wydłuża trwałość L4** (325 → 416, ok. +28%); samo żerowanie gradientowe
  ma niewielki wpływ. **L5 pozostaje bez zmian** (~644–671) — potwierdza wniosek: zachowania
  pomagają środkowi łańcucha, ale **nie ratują wierzchołka**.

### 2.3. Wpływ metabolizmu — `exp3_metabolism.csv`
| metabolizm × | L4 persist | L5 persist |
|---|---|---|
| 0.6 | 508 | **1152** |
| 0.8 | 446 | 845 |
| 1.0 | 451 | 651 |
| 1.2 | 366 | 529 |
| 1.4 | 311 | 460 |

- **Najsilniejsza dźwignia.** Niższy koszt metaboliczny dramatycznie wydłuża trwałość górnych
  poziomów (L5: 1152 przy ×0.6 vs 460 przy ×1.4 — relacja monotoniczna). To intuicyjne: mniejszy
  koszt życia = wolniejsze głodzenie = dłuższe przetrwanie przy niedoborze pokarmu.

**Podsumowanie:** trwałość górnych poziomów rośnie z (a) wydajnością transferu i (b) niższym
metabolizmem, a stadność pomaga warstwie środkowej. Żaden z czynników nie zapewnia jednak trwałego
przetrwania L5 — pod twardą regułą 10% i bez odradzania bazy producentów **kolaps jest nieunikniony**,
zmienia się jedynie jego tempo. To jest właśnie ilustracja niskiego prawdopodobieństwa przetrwania
piątego poziomu troficznego.

---

## 3. Planowane usprawnienia (dalsze prace)

Do wprowadzenia w przyszłości — najlepiej jako **przełączalne/parametryzowalne**, by mierzyć ich wpływ:

1. **Tłowe odradzanie trawy (`backgroundSpawn`)** — mała szansa na pojawienie się nowego
   producenta w losowym miejscu na krok (rozsiew z zewnątrz / bank nasion). Umożliwia **odbudowę
   ekosystemu** po przeżarciu, a więc trwałe oscylacje zamiast jednokierunkowego kolapsu.
2. **Skorelowane błądzenie + reguła „rezygnacji" (dispersal/giving-up)** — utrzymywanie kierunku
   przez kilka kroków i przejście w tryb dalekiej wędrówki, gdy od K kroków nie widać ofiary.
   Likwiduje zastygłe klastry i pozwala szukać pokarmu w innych rejonach.
3. **Spójność martwych agentów** — dodać strażnik `isAlive` także producentowi (obecnie zjadany
   producent może w tym samym kroku jeszcze raz się rozsiać; efekt pomijalny, ale dla porządku).
4. **Dziedziczenie cech + mutacja (ewolucja / ML)** — przenieść część parametrów na osobnika i
   dodać dziedziczenie z mutacją; otwiera pytania o **selekcję naturalną** (np. czy presja
   drapieżników wyselekcjonuje szybsze ofiary). Naturalny kierunek na sekcję „Further Research".
