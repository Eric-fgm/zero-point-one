# Dokumentacja projektu — Łańcuch pokarmowy z regułą 10% energii

Model agentowy (ABM) symulujący 5-poziomowy łańcuch pokarmowy, w którym każdy drapieżnik
pozyskuje tylko **10%** energii zjedzonej ofiary. Celem jest pokazanie wahań liczebności
populacji wynikających z reguły 10% oraz prawdopodobieństwa przetrwania piątego poziomu
troficznego (drapieżnika szczytowego).

**Zespół:** Eryk Hadała, Hieronim Koc, Kacper Drożdż
**Technologie:** Kotlin Multiplatform, Compose Multiplatform (Desktop/JVM), Kotlin Coroutines, StateFlow.

> Ten dokument opisuje *co* i *jak* jest zaimplementowane: definicję agentów (nasze podejście),
> architekturę kodu, działanie symulatora/GUI, instrukcję ustawiania parametrów oraz sposób
> interpretacji wyników. Powiązane materiały: `MODEL.md` (uzasadnienie modelu, pytanie badawcze),
> `PROGRESS.md` (mapa drogowa, kamienie milowe).

---

## Spis treści
1. [Jak uruchomić](#1-jak-uruchomić)
2. [Definicja agentów — nasze podejście](#2-definicja-agentów--nasze-podejście)
3. [Reguła 10% i bilans energii](#3-reguła-10-i-bilans-energii)
4. [Zachowania agentów](#4-zachowania-agentów)
5. [Architektura i implementacja](#5-architektura-i-implementacja)
6. [Opis programu / symulatora (GUI)](#6-opis-programu--symulatora-gui)
7. [Instrukcja parametrów (how-to)](#7-instrukcja-parametrów-how-to)
8. [Jak rozumieć dane i wyniki](#8-jak-rozumieć-dane-i-wyniki)
9. [Tabela parametrów domyślnych](#9-tabela-parametrów-domyślnych)

---

## 1. Jak uruchomić

Wymagany jest JDK (17+; projekt testowany na Javie 21). Gradle pobiera się sam przez wrapper.

```bash
./gradlew :composeApp:run
```

Otworzy się okno aplikacji. W panelu bocznym (po lewej) wpisujemy **Width** i **Height**
(np. 800 × 600), opcjonalnie ustawiamy pozostałe parametry, po czym klikamy **Run**.
Pola są wstępnie wypełnione sensownymi wartościami domyślnymi, więc można od razu kliknąć Run.

---

## 2. Definicja agentów — nasze podejście

W naszym modelu **agent** to autonomiczny, dyskretny organizm umieszczony w dwuwymiarowym
środowisku pikselowym. Każdy agent posiada własny budżet energetyczny i samodzielnie podejmuje
decyzje na podstawie **lokalnej percepcji** (widzi tylko to, co znajduje się w jego promieniu
percepcji). Centralny silnik jedynie wywołuje agentów po kolei — nie steruje nimi globalnie.

Agenci są klasyfikowani **wyłącznie według poziomu troficznego** (1–5). W kodzie reprezentuje
to klasa zapieczętowana (`sealed class`) `Agent` z dwiema podklasami:

| Klasa w kodzie | Poziom | Rola ekologiczna | Zjada | Jest zjadany przez |
|---|---|---|---|---|
| `Agent.Producer` | 1 | Producent / autotrof (roślinność) | — (fotosynteza) | poziom 2 |
| `Agent.Consumer` (Primary) | 2 | Konsument I rzędu (roślinożerca) | poziom 1 | poziom 3 |
| `Agent.Consumer` (Secondary) | 3 | Konsument II rzędu (mały drapieżnik) | poziom 2 | poziom 4 |
| `Agent.Consumer` (Tertiary) | 4 | Konsument III rzędu (duży drapieżnik) | poziom 3 | poziom 5 |
| `Agent.Consumer` (Quaternary) | 5 | Konsument IV rzędu (drapieżnik szczytowy) | poziom 4 | — |

Poziom konsumenta jest opisany typem wyliczeniowym `ConsumerLevel` (`Primary`..`Quaternary`),
który mapuje się na numer poziomu troficznego 2–5.

### Cechy (zmienne stanu) każdego agenta

Każdy agent przechowuje:
- **`position`** — pozycję (x, y) w pikselach,
- **`energy`** — bieżącą energię (maleje co krok, rośnie po jedzeniu).

Pozostałe cechy są zależne od poziomu i przechowywane centralnie w `SpeciesParams`
(jeden komplet parametrów na poziom), dzięki czemu sam agent pozostaje „lekki", a wszystkie
liczby są w jednym miejscu (łatwe strojenie i eksperymenty):

| Cecha (`SpeciesParams`) | Znaczenie |
|---|---|
| `maxEnergy` | Maksymalna pojemność energetyczna (rośnie z poziomem troficznym) |
| `initialEnergy` | Energia początkowa nowo utworzonego agenta |
| `metabolicRate` | Energia tracona w każdym kroku (koszt życia; 0 dla producentów) |
| `reproductionThreshold` | Próg energii, powyżej którego agent się rozmnaża |
| `reproductionCost` | Koszt energetyczny rozmnożenia (staje się energią potomka) |
| `perceptionRadius` | Promień (w pikselach) skanowania otoczenia w poszukiwaniu ofiar/drapieżników |
| `moveSpeed` | Liczba pikseli pokonywana w jednym kroku |
| `regrowRate` | (tylko producent) energia odzyskiwana co krok — fotosynteza |
| `spawnChance` | (tylko producent) szansa na rozsianie nowego producenta na krok |

### Środowisko jako agent pasywny
Siatka heksagonalna (`Terrain`) pełni rolę pasywnego, niebiologicznego agenta: przechowuje
granice świata i umożliwia szybkie zapytania o sąsiedztwo (kto jest w pobliżu), niezbędne do
wykrywania zdarzeń konsumpcji.

### Relacje między agentami
- **Ścisła stratyfikacja:** agent poziomu *n* rozpoznaje i zjada wyłącznie agentów poziomu *n−1*.
- **Jednokierunkowy przepływ energii:** energia płynie tylko „w górę" łańcucha. Każde załamanie
  na poziomie producentów kaskadowo wpływa na wszystkie wyższe poziomy (sterowanie oddolne).
- **Lokalność:** relacje są geograficzne — do zjedzenia konieczna jest bliskość przestrzenna.
- **Konkurencja wewnątrzgatunkowa (pośrednia):** agenci tego samego poziomu nie polują na siebie,
  ale rywalizują o tę samą, skończoną pulę ofiar w okolicy (samoregulacja przez pojemność środowiska).

---

## 3. Reguła 10% i bilans energii

Reguła 10% (prawo Lindemana) jest **głównym mechanizmem** napędzającym cały model.

- **Wydajność transferu** (`SimulationConfig.energyTransferYield`, domyślnie `0.10`): przy zjedzeniu
  ofiary drapieżnik zyskuje dokładnie 10% **bieżącej** energii ofiary (ograniczone do `maxEnergy`),
  a ofiara zostaje usunięta. Pozostałe ~90% jest „tracone" (ciepło/metabolizm) i nigdy nie trafia wyżej.
  W naszej wersji parametr ten jest **konfigurowalny** (suwak w GUI), aby można było zbadać jego wpływ.

Pełny bilans energii pojedynczego konsumenta w jednym kroku:
1. `energy -= metabolicRate` (koszt życia),
2. jeśli `energy <= 0` → **śmierć głodowa** (agent usuwany),
3. jeśli zjadł ofiarę: `energy += energyTransferYield * energiaOfiary` (do `maxEnergy`),
4. jeśli `energy >= reproductionThreshold`: **rozmnożenie** — powstaje potomek obok rodzica,
   `energy -= reproductionCost` (ten koszt staje się energią początkową potomka).

Producent (poziom 1) zamiast polować:
- `energy = min(maxEnergy, energy + regrowRate)` (fotosynteza co krok),
- jeśli liczba producentów < pojemność środowiska oraz `energy >= reproductionThreshold` oraz
  `random < spawnChance` → rozsiewa nowego producenta w pobliżu.

**Skutek piramidy energii:** ponieważ 90% energii ginie na każdym szczeblu, utrzymanie jednego
agenta wyższego poziomu wymaga wielokrotnie większej biomasy poziomów niższych. Stąd górne
poziomy (zwłaszcza 5) są z natury kruche — i to jest istota badania.

---

## 4. Zachowania agentów

Ruch konsumentów to **mieszanka wektorów sterujących** (model w stylu boids). W każdym kroku
agent wyznacza wektor „pożądanego" ruchu i przesuwa się o `moveSpeed` w jego kierunku.
Priorytety (kod: `Agent.Consumer.update`):

1. **Ucieczka (flee)** — jeśli w zasięgu są drapieżniki (poziom *n+1*), agent oblicza wektor
   odsuwający się od *wszystkich* z nich (bliżsi ważą mocniej — waga ~1/d²) i ucieka. Przetrwanie
   ma pierwszeństwo przed jedzeniem.
2. **Jedzenie (eat)** — w przeciwnym razie, jeśli najbliższa ofiara (poziom *n−1*) jest w odległości
   ≤ `CONSUME_DISTANCE` (14 px), następuje **zdarzenie konsumpcji** (reguła 10%, ofiara ginie).
3. **Sterowanie (steer)** — w przeciwnym razie agent łączy:
   - **polowanie** — w stronę ofiar; przy włączonym **żerowaniu gradientowym** (`gradientForaging`)
     kieruje się ku *zagęszczeniu* ofiar (suma ważona odwrotnością odległości), a nie tylko ku
     najbliższej sztuce,
   - **stadność** (`herding`) — **kohezja** (ku środkowi masy sąsiadów tego samego poziomu) plus
     **separacja** (odpychanie od zbyt bliskich sąsiadów, aby stado nie skupiło się w jednym punkcie),
   - jeśli żaden wektor nie powstał → **błądzenie losowe** (wander).

Zachowania `herding` i `gradientForaging` są **przełączalne** (przełączniki w GUI oraz w
`BehaviorConfig`), co pozwala mierzyć ich wpływ w eksperymentach.

### Oczekiwane zjawiska emergentne
- **Cykle Lotki–Volterry** — naturalne wahania liczebności (widoczne zwłaszcza dla par L1–L2).
- **Skupianie się (stada)** — agenci niższych poziomów tworzą luźne klastry (kohezja + separacja).
- **Optymalizacja żerowania** — drapieżniki podążają za zagęszczeniem ofiar (gradient).

---

## 5. Architektura i implementacja

Logika symulacji jest czystym kodem Kotlin, całkowicie **oddzielonym od UI**. Interfejs
obserwuje stan reaktywnie przez `StateFlow` i renderuje niezmienne „migawki" (`SimulationSnapshot`).

```
engine/
  Agent.kt            sealed Agent: Producer (L1) + Consumer (L2..L5). Energia + update() z wektorami sterującymi.
  SimulationConfig.kt SpeciesParams na poziom + BehaviorConfig + populacje początkowe, pojemność, yield, seed, speed.
  Engine.kt           centralna pętla krokowa (korutyna). Posiada listę agentów. Publikuje migawki przez StateFlow.
  Terrain.kt          geometria świata (rozmiar + komórki heksagonalne do rysowania) + budowa indeksu przestrzennego.
  SpatialIndex.kt     wyszukiwanie sąsiadów w promieniu percepcji (heksagonalny hash przestrzenny).
  StepContext.kt      kontekst kroku: indeks, config, kolejki narodzin/śmierci, liczebności.
  Snapshot.kt         niezmienne SimulationSnapshot (dane do rysowania + liczebności + historia) dla UI.
enums/ConsumerLevel.kt  Primary..Quaternary → poziomy troficzne 2..5.
utils/                  Position (+ funkcje wektorowe), Vec2 (sterowanie), Hexagon (matematyka siatki).
views/                  UI Compose: App, Sidebar, HexagonGrid, AgentsView, PopulationChart, PopulationOverlay, LevelPalette.
```

### Centralna pętla krokowa (kluczowa decyzja projektowa)
Prototyp (PoC) uruchamiał **jedną korutynę na agenta**. Gdy agenci zaczynają na siebie
oddziaływać (wspólny odczyt/zapis siatki, zjadanie się), powoduje to **wyścigi danych**.
Dlatego zastąpiliśmy to **jednym dyskretnym harmonogramem** (`Engine`): w każdym kroku (tick)
silnik:
1. buduje świeży **indeks przestrzenny** wszystkich agentów,
2. pozwala każdemu agentowi działać w **losowej kolejności** (aby uniknąć obciążenia kierunkowego),
3. stosuje zakolejkowane **narodziny i śmierci atomowo** na końcu kroku.

Dzięki temu przebieg jest **deterministyczny** dla danego ziarna (`seed`), wolny od wyścigów,
a sterowanie prędkością staje się trywialne. To standardowe podejście ABM (jak w NetLogo/Mesa).

`Engine` udostępnia też `initialize()` i `advance()` (jeden krok) — używane bezgłowo (bez UI)
w testach i w przyszłym trybie wsadowym (eksperymenty M4).

### Przepływ danych do UI
`Engine` → `MutableStateFlow<SimulationSnapshot>` → `App` zbiera przez `collectAsState()` →
widoki renderują migawkę. Migawka zawiera dane agentów (id, poziom, x, y, stosunek energii),
liczebności na poziom oraz okno historii (ostatnie 600 kroków) do wykresu.

---

## 6. Opis programu / symulatora (GUI)

Układ aplikacji: **panel boczny** (sterowanie) | **obszar świata** (góra) + **wykres** (dół).

### 6.1. Panel boczny (Sidebar)
Od góry: rozmiar świata, ziarno, populacje początkowe, pojemność łąki, transfer energii,
metabolizm, przełączniki zachowań, prędkość oraz przyciski sterujące i eksport. Szczegóły
w sekcji [7](#7-instrukcja-parametrów-how-to).

### 6.2. Obszar świata
- **Siatka heksagonalna** (szare tło) — wizualizacja przestrzeni i indeksu sąsiedztwa.
- **Kropki = agenci.** Kolor zależy od poziomu troficznego, rozmiar rośnie z poziomem,
  a **jasność (przezroczystość) odzwierciedla poziom energii** (jaśniejszy = bardziej najedzony):

  | Poziom | Kolor | Opis |
  |---|---|---|
  | L1 | 🟢 zielony | producenci |
  | L2 | 🟡 żółty | roślinożercy |
  | L3 | 🟠 pomarańczowy | małe drapieżniki |
  | L4 | 🔴 czerwony | duże drapieżniki |
  | L5 | 🟣 fioletowy | drapieżnik szczytowy |

- **Nakładka liczebności** (lewy górny róg): bieżący `tick`, suma agentów (`total`) oraz
  liczebność każdego poziomu (w jego kolorze).
- Obszar jest przewijalny (gdy świat jest większy niż okno).

### 6.3. Wykres populacji (dolny panel)
Wykres liniowy liczebności w czasie, jedna linia na poziom (kolory jak wyżej).
**Oś Y jest logarytmiczna** — patrz sekcja [8](#8-jak-rozumieć-dane-i-wyniki).

---

## 7. Instrukcja parametrów (how-to)

Wszystkie parametry oprócz **Speed**, **Pause** i **Export** są stosowane dopiero po kliknięciu
**Run/Restart** (rozpoczynają nową symulację). **Speed** i **Pause** działają „na żywo".

| Kontrolka w GUI | Co ustawia | Co się zmienia, gdy ją zwiększymy/przełączymy |
|---|---|---|
| **Width / Height** | Rozmiar świata w pikselach | Większy świat = więcej miejsca i **mniejsze zagęszczenie** przy tej samej liczbie agentów (trudniej spotkać ofiarę). |
| **Seed** | Ziarno generatora losowego | Ten sam seed + te same parametry ⇒ **identyczny przebieg** (powtarzalność). Zmiana seeda = inny przebieg — używaj wielu seedów do uśredniania. |
| **L1 … L5** | Początkowa liczba agentów na poziom | Więcej agentów startowych danego poziomu = większa szansa, że poziom „złapie się" na starcie. Zbyt mało L4/L5 ⇒ szybkie wymarcie. |
| **Grass capacity** (puste = auto) | Maks. liczba producentów (pojemność środowiska) | Większa pojemność = większa **baza energetyczna** ⇒ więcej poziomów może przetrwać. Puste = auto z powierzchni (`powierzchnia/2500`, zakres 60–500). |
| **Energy transfer %** | Reguła 10% — ile % energii ofiary trafia do drapieżnika | **Najważniejszy parametr.** Wyższy % (np. 20–30) = mniejsze straty na szczeblu ⇒ wyższe poziomy żyją **znacznie dłużej**. 5% = niemal natychmiastowy kolaps góry. |
| **Metabolism ×** | Mnożnik tempa metabolizmu konsumentów (0.5–1.5) | Niższy = wolniejsze głodzenie ⇒ **łatwiejsze przetrwanie** (zwłaszcza górnych poziomów). Wyższy = szybsze wymieranie. |
| **Herding** (przełącznik) | Stadność (kohezja + separacja) | Włączona = ofiary tworzą **stada**; nieco większa przeżywalność biomasy i poziomu L4 (efekt rozcieńczenia drapieżnictwa). |
| **Gradient foraging** (przełącznik) | Żerowanie ku zagęszczeniu ofiar | Włączone = drapieżniki celują w **skupiska** ofiar, a nie w pojedyncze sztuki ⇒ skuteczniejsze polowanie, dłużej utrzymują się poziomy 3–4. |
| **Speed** (suwak, na żywo) | Prędkość symulacji | W prawo = szybsze kroki (do ~20 ms/krok), w lewo = wolniejsze (do ~200 ms/krok). Nie wpływa na wynik, tylko na tempo obserwacji. |
| **Run / Restart** | Start / restart z bieżącymi parametrami | Tworzy nową symulację (kasuje poprzednią). |
| **Pause / Resume** | Pauza / wznowienie | Zatrzymuje czas bez utraty stanu — wygodne do oglądania konkretnej chwili. |
| **Reset** | Czyści symulację | Usuwa bieżącą symulację (puste okno świata). |
| **Export CSV** | Zapis historii populacji | Tworzy plik `foodchain_populations_tick<N>.csv` w katalogu roboczym (ścieżka pokazana w panelu). |

---

## 8. Jak rozumieć dane i wyniki

### 8.1. Wykres (dlaczego skala logarytmiczna)
Liczebności poziomów różnią się o **rzędy wielkości** (setki producentów vs. pojedyncze sztuki
drapieżnika szczytowego). Na skali liniowej linie górnych poziomów byłyby niewidoczne (płaskie
przy dnie). Dlatego oś Y jest **logarytmiczna** — wszystkie poziomy są czytelne jednocześnie,
a sam kształt piramidy (rzędy wielkości między poziomami) jest dodatkową informacją.

### 8.2. Co obserwować
- **Oscylacje L1–L2:** producenci i roślinożercy falują wzajemnie (klasyka Lotki–Volterry).
- **Kaskada troficzna (wymieranie od góry):** przy ustawieniach domyślnych poziomy wymierają
  w kolejności od najwyższego — najpierw L4 (~krok 250–400), potem L3, a w końcu (po utracie
  drapieżników kontrolujących roślinożerców) następuje **przeżarcie producentów** i całkowity
  kolaps. To poprawny, pouczający wynik: im wyższy poziom, tym mniejsze prawdopodobieństwo przetrwania.
- **Los L5 (cel badania):** zwykle L5 nie utrzymuje polującej populacji — jego ofiara (L4) znika
  zbyt wcześnie, więc L5 powoli głodzi się na energii początkowej. To bezpośrednia ilustracja
  „kruchości drapieżnika szczytowego" wynikającej z reguły 10%.

### 8.3. Wstępne wyniki (z eksperymentów porównawczych)
- Włączenie stadności i żerowania gradientowego wydłuża przeciętną przeżywalność **L4** o ~12%
  i zwiększa ocalałą biomasę — ale **nie ratuje L5** (jego los zależy od poziomu poniżej).
- Zwiększanie **Energy transfer %** jest najsilniejszą dźwignią pozwalającą utrzymać wyższe
  poziomy dłużej — to materiał na kluczowy wykres w raporcie (przeżywalność L5 vs. wydajność transferu).

### 8.4. Format pliku CSV
Eksport zapisuje liczebności na każdy krok:

```
tick,L1,L2,L3,L4,L5,total
0,80,45,20,9,4,158
1,...
```

Plik nadaje się bezpośrednio do wykresów w raporcie (np. w Pythonie/Excelu): kolumna `tick`
jako oś czasu, kolumny `L1..L5` jako serie. Sekcja analizy wyników w raporcie jest oceniana
najwyżej — warto porównać przebiegi dla różnych `Energy transfer %`, `seed` i przełączników zachowań.

---

## 9. Tabela parametrów domyślnych

Wartości z `SimulationConfig.default(...)` (świat 800 × 600). Energia bezwymiarowa, dystanse w pikselach.

| Poziom | maxEnergy | initialEnergy | metabolicRate | reproThreshold | reproCost | perception | moveSpeed | (regrow / spawnChance) | Populacja pocz. |
|---|---|---|---|---|---|---|---|---|---|
| L1 producent | 200 | 100 | 0 | 140 | 70 | – | – | 3.0 / 0.05 | 80 |
| L2 roślinożerca | 150 | 80 | 0.4 | 110 | 55 | 80 | 6 | – | 45 |
| L3 mały drap. | 300 | 150 | 0.5 | 220 | 110 | 100 | 7 | – | 20 |
| L4 duży drap. | 500 | 250 | 0.6 | 380 | 190 | 120 | 8 | – | 9 |
| L5 szczytowy | 800 | 400 | 0.7 | 650 | 320 | 140 | 9 | – | 4 |

Pozostałe domyślne:
- **Reguła transferu (`energyTransferYield`):** 0.10 (10%).
- **Pojemność środowiska:** `powierzchnia / 2500`, ograniczona do 60–500 (dla 800×600 ≈ 192).
- **Zachowania:** `herding` = wł., `gradientForaging` = wł.,
  `cohesionWeight` = 0.6, `separationWeight` = 0.9, `separationRadius` = 18 px.
- **Dystans konsumpcji (`CONSUME_DISTANCE`):** 14 px.
- **Okno wykresu:** ostatnie 600 kroków.
- **Prędkość (suwak):** krok od ~20 ms (szybko) do ~200 ms (wolno).

> Uwaga: domyślne parametry są rozsądnym punktem wyjścia, nie są „dostrojone" pod konkretny wynik.
> Przy twardej regule 10% poziomy 4–5 są celowo kruche — badanie warunków ich przetrwania
> (przez strojenie i przemiatanie parametrów) jest właśnie celem fazy eksperymentów.
