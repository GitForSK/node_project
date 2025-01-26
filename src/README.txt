Dany program działa na protokole TCP. Każdy węzeł jest tworzony w oddzielnym wątku.
Stworzony węzeł łączy się z innym węzłem jeśli został podany gateway, a potem 
nadsłuchuje połączenia z timeoutem 1500 ms. W momencie, gdy załapie połączenia tworzy wątek, 
który obsłuży to połączenie. Jeśli jest to inny node domagający się połączenia węzeł dostanie
wiadomość HELLO, po czym węzły wymienią się id i dodadzą się w connectedNodes. Jeśli jest to
klient proszący o zasoby to jeśli węzeł ma to daje od razu. Jeśli nie to najpierw przeszukuje
inne węzły. Jeśli taki zasób znajdzie się tworzy się wątek, który jest odpowiedzialny za 
alokacje zasobów. Jeśli znajdzie wszystkie zasoby to wątek, który został poproszony o zasoby
wyśle do wszystkich węzłów commendę ACTIVATE_ALLOCATOR co powoduje alokacje zasobów oraz 
usunięcie wyczyszczenie wątków odpowiedzialnych za alokacje (W momencie jak węzeł nie ma
wątka do alokacji nie robi nic, tylko wysyła wiadomość dalej). Jeśli nie znajdzie wszystkich 
zasobów wyczyści wątki odpowiedzialne za alokacjie i odeśle klientowi wiadomość FAILED.

Ważne: Jak id NetworkNoda już istnieje, to program go nie stworzy, więc w skrypcie 2.0.2
pierwszy węzeł się nie zamknie gdyż wysyłamy żdanie TERMINATE do nie stworzonego węzła.

Kompilacja:
javac NetworkNode.java
javac NetworkClient.java

Użycie:
start java NetworkNode -ident <identyfikator> -tcpport <numer portu TCP> -gateway <adres>:<port> <lista zasobów (wzór X:N, gdzie X to duża litera, a N to liczba)>
java NetworkClient -ident <identyfikator> -gateway <adres>:<port> <lista zasobów (wzór X:N, gdzie X to duża litera, a N to liczba)>

Najpierw tworzymy wezeł bez gateway. Następnie możemy do niego podłaczyć kolejne wezły podając 
gateway. Potem możemy podłączyć klienta i poprosić wezły o zasoby. Jeśli się powiedzie 
dostaniemy informacje na którym węźle dany zasób jest zarezerwowany, jeśli się nie uda to
dostajemy informację FAILED.
