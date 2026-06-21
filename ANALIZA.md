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
**uśrednia po wielu ziarnach** i zapisuje CSV do `./results`. Poniżej wyniki dla świata 800×600,
1500 kroków, **30 ziaren**. **Metryka:** `persist` = średni ostatni krok, w którym poziom jeszcze
istniał (im większy, tym dłużej poziom przetrwał). Wartości podajemy jako **średnia ± odchylenie
standardowe** po 30 ziarnach; na wykresach odpowiadają im **wąsy/słupki błędu (±1 SD)**. Rozrzut jest
tu kluczowy — pokazuje, czy różnica między ustawieniami to realny sygnał, czy mieści się w szumie.

> Wykresy poniżej generuje `python3 scripts/plot_sweep.py` (czyta `results/*.csv`, zapisuje PNG do
> `results/`). Wersja angielska pod raport: `python3 scripts/plot_sweep.py` → `*_en.png`;
> wersja polska (osadzona tu): `--lang pl` → `*_pl.png`. Kolory zgodne z paletą GUI.

### 2.1. Przeżywalność a wydajność transferu energii (reguła 10%) — `exp1_yield.csv`
Trwałość (`persist`) jako **średnia ± SD** [ticki]:

| yield | L1 | L2 | L3 | L4 | L5 |
|---|---|---|---|---|---|
| 0.05 | 1168±479 | 678±111 | 328±12 | 376±87 | **614±15** |
| 0.08 | 828±559 | 593±74 | 368±51 | 396±81 | 637±18 |
| 0.10 | 753±541 | 586±107 | 399±63 | 385±98 | 653±20 |
| 0.15 | 826±564 | 593±91 | 507±140 | 423±88 | 709±38 |
| 0.20 | 394±310 | 558±84 | 539±127 | 437±93 | 746±49 |
| 0.25 | 689±586 | 525±73 | 569±111 | 436±125 | 822±75 |
| 0.30 | 374±387 | 499±66 | 610±123 | 460±146 | **853±84** |

- **Kluczowy wynik (mocny):** trwałość drapieżnika szczytowego (L5) **rośnie monotonicznie** z
  wydajnością transferu — 614 → 853 kroków, gdy yield rośnie 5% → 30%. Wąsy błędu L5 są wąskie
  (±15–84) względem tego wzrostu, więc to **realny sygnał**, a nie szum. Bezpośrednie potwierdzenie
  reguły 10%: im mniej energii ginie na szczeblu, tym dłużej utrzymuje się wierzchołek piramidy.
- **Drugi sygnał:** trwałość L3 również rośnie z yield (328 → 610) — wydajniejszy transfer zasila
  środek łańcucha.
- **Niuans (słaby, duży rozrzut):** trawa (L1) wykazuje *tendencję* spadkową przy wyższym yield
  (wydajniejsze drapieżniki → więcej roślinożerców → silniejszy wypas), ale jej `persist` jest
  **bardzo zaszumiony** — SD bywa rzędu samej średniej (np. 689±586). Traktujemy to jako trend, nie
  jako pewny wniosek.
- **Uwaga metodologiczna:** udział „przeżywających do końca" (kolumny `*_surv`) wynosi 0 dla
  wszystkich konsumentów przy każdym yield — bo bez odradzania trawy (pkt 1.2) **kolaps jest
  nieunikniony** do kroku 1500. Dlatego sensowną miarą jest *czas trwania*, a nie binarne
  przeżycie. To wprost motywuje usprawnienie `backgroundSpawn` (pkt 2 → „Planowane usprawnienia").

![Trwałość poziomów a wydajność transferu energii](results/exp1_yield_pl.png)

### 2.2. Wpływ zachowań (stadność × żerowanie gradientowe) — `exp2_behaviours.csv`
Trwałość (`persist`) jako **średnia ± SD** [ticki]:

| herding | gradient | L2 | L3 | L4 | L5 |
|---|---|---|---|---|---|
| nie | nie | 630±138 | 378±53 | 372±117 | 661±21 |
| nie | tak | 623±87 | 392±58 | 382±92 | 657±23 |
| tak | nie | 538±108 | 368±37 | 399±97 | 653±23 |
| tak | tak | 611±88 | 389±63 | 412±77 | 651±23 |

- **Rewizja po zwiększeniu liczby ziaren (15 → 30):** przy pokazanym rozrzucie **żadna z czterech
  konfiguracji nie różni się w sposób istotny**. Dla L4 wartości 372 → 412 mieszczą się w granicach
  ±1 SD (~77–117), więc obserwowana wcześniej „przewaga stadności o ~28%" **była artefaktem małej
  próby** — nie jest odporna na zmianę ziaren.
- **L5 płaskie i pewne** (~651–661, SD ~21–23) — zachowania nie wpływają na trwałość wierzchołka.
- **Wniosek:** stadność i żerowanie gradientowe zmieniają **strukturę przestrzenną** (klastry,
  formowanie stad — patrz pkt 1.3), ale **nie zmieniają czasu przetrwania** pod twardą regułą energii.
  To samo w sobie jest wynikiem: emergentne ruchy nie ratują populacji, gdy o losie decyduje bilans energii.

![Wpływ zachowań na trwałość](results/exp2_behaviours_pl.png)

### 2.3. Wpływ metabolizmu — `exp3_metabolism.csv`
Trwałość (`persist`) jako **średnia ± SD** [ticki]:

| metabolizm × | L2 | L3 | L4 | L5 |
|---|---|---|---|---|
| 0.6 | 823±136 | 809±191 | 601±230 | **1136±63** |
| 0.8 | 709±96 | 526±108 | 467±130 | 835±36 |
| 1.0 | 614±56 | 395±77 | 406±82 | 651±21 |
| 1.2 | 537±69 | 302±36 | 366±37 | 537±19 |
| 1.4 | 514±72 | 246±10 | 314±26 | **459±16** |

- **Najsilniejsza i najbardziej odporna dźwignia.** Niższy koszt metaboliczny dramatycznie wydłuża
  trwałość wszystkich poziomów (L5: 1136 przy ×0.6 vs 459 przy ×1.4 — monotonicznie, a wąsy błędu
  L5 nie nachodzą na siebie wzdłuż zakresu). Mniejszy koszt życia = wolniejsze głodzenie = dłuższe
  przetrwanie przy niedoborze pokarmu.

![Wpływ metabolizmu na trwałość](results/exp3_metabolism_pl.png)

**Podsumowanie:** trwałość górnych poziomów rośnie **w sposób odporny** z (a) niższym metabolizmem
(efekt najsilniejszy) i (b) wyższą wydajnością transferu energii (zwłaszcza dla L5 i L3). Natomiast
**zachowania (stadność/gradient) nie są istotnym czynnikiem** trwałości — ich różnice toną w rozrzucie
między ziarnami (to lekcja z przejścia 15 → 30 ziaren i pokazania ±SD). Żaden czynnik nie zapewnia
trwałego przetrwania L5 — pod twardą regułą 10% i bez odradzania bazy producentów **kolaps jest
nieunikniony**, zmienia się jedynie jego tempo. To jest właśnie ilustracja niskiego prawdopodobieństwa
przetrwania piątego poziomu troficznego.

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
