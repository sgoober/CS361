import random
import time

suits = ('Hearts', 'Diamonds', 'Spades', 'Clubs')
ranks =('Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten', 'Jack', 'Queen', 'King', 'Ace')
values ={'Two':2, 'Three':3, 'Four':4, 'Five':5, 'Six':6, 'Seven':7, 'Eight':8, 'Nine':9, 'Ten':10, 'Jack':10,
         'Queen':10, 'King':10, 'Ace':11} 

import zmq

class Card():
    
    def __init__(self,suit,rank):
        self.suit=suit
        self.rank=rank

    def __str__(self):
        return(str(self.rank + " of " + self.suit))
      
class Deck:
    
    def __init__(self):
        self.deck = []
        for suit in suits:
            for rank in ranks:
                self.deck.append(Card(suit,rank))

    def shuffle(self):
        random.shuffle(self.deck)
        
    def deal(self):
        card = self.deck.pop()
        return card
    
    def __str__(self):
        deck_comp=''
        for card in self.deck:
            deck_comp+='\n'+card.__str__()
        return 'the deck has:'+ deck_comp   

class Hand:
    
    def __init__(self):
        self.cards = []
        self.value = 0
        self.aces = 0
    
    def add_card(self,card):
        self.cards.append(card)
        self.value += values[card.rank]
        if card.rank == 'Ace':
            self.aces += 1
    
    def adjust_for_ace(self):
        while self.value > 21 and self.aces:
            self.value -= 10
            self.aces -= 1

def main():
    playing = True
    print("Game start")
    
    seed_state = random.getstate()
    seed = seed_state[1][seed_state[0]]
    print("seed:", seed)
    seed_tuple = str(seed)
    



    deck = Deck()
    deck.shuffle()
    
    player_hand = Hand()
    player_hand.add_card(deck.deal())
    player_hand.add_card(deck.deal())
    
    dealer_hand = Hand()
    dealer_hand.add_card(deck.deal())
    dealer_hand.add_card(deck.deal())
    
    socket.send(b"dealers")
    print("sent: dealers")
    ignore = socket.recv()
    print("received: ", ignore)
    for i in range(len(dealer_hand.cards)):
        card = str(dealer_hand.cards[i])
        socket.send(card.encode())
        print("sent: dealer's ", card)
        ignore = socket.recv()
        print("received: ", ignore)
        
    socket.send(b"players")
    print("sent: players")
    ignore = socket.recv()
    print("received: ", ignore)
    for i in range(len(player_hand.cards)):
        card = str(player_hand.cards[i])
        socket.send(card.encode())
        print("sent: player's ", card)        
        ignore = socket.recv()
        print("received: ", ignore)
    while playing:
        
        # Get player choice from ingame choice
        socket.send(b'decide')
        print("sent: decide")
        x = socket.recv()
        print("received: ", x)
        
        if x == b'hit':
            single_card=deck.deal()
            player_hand.add_card(single_card)
            player_hand.adjust_for_ace()

        else:
            playing = False
        
        # send card information for players turns        
        socket.send(b"dealers")
        print("sent: dealers")
        ignore = socket.recv()
        print("received: ", ignore)
        for i in range(len(dealer_hand.cards)):
            card = str(dealer_hand.cards[i])
            print("sent: dealer's ", card)
            socket.send(card.encode())
            ignore = socket.recv()
            print("received: ", ignore)
        socket.send(b"players")
        print("sent: players")
        ignore = socket.recv()
        print("received: ", ignore)
        for i in range(len(player_hand.cards)):
            card = str(player_hand.cards[i])
            print("sent: player's ", card)
            socket.send(card.encode())
            ignore = socket.recv()
            print("received: ", ignore)
        
        if player_hand.value > 21:
            print("player loses")
            socket.send(b'loss')
            ignore = socket.recv()
            socket.send(seed_tuple.encode()) #Send seed so game's randomness is provable
            break
    
    if player_hand.value <= 21:
        while dealer_hand.value < 17:
            single_card=deck.deal()
            dealer_hand.add_card(single_card)
            dealer_hand.adjust_for_ace()
            
        # send card information for dealers turns
        socket.send(b"dealers_unhidden")
        ignore = socket.recv()
        print("received: ", ignore)
        for i in range(len(dealer_hand.cards)):
            card = str(dealer_hand.cards[i])
            print("sent: dealer's ", card)
            socket.send(card.encode())
            ignore = socket.recv()
            print("received: ", ignore)
        socket.send(b"players")
        ignore = socket.recv()
        print("received: ", ignore)
        for i in range(len(player_hand.cards)):
            card = str(player_hand.cards[i])
            print("sent: player's ", card)
            socket.send(card.encode())
            ignore = socket.recv()
            print("received: ", ignore)
         
        # Win conditions
        if dealer_hand.value > 21:
            print("player wins")
            socket.send(b'win')
            ignore = socket.recv()
            socket.send(seed_tuple.encode()) #Send seed so game's randomness is provable
        elif dealer_hand.value > player_hand.value:
            print("player loses")
            socket.send(b'loss')
            ignore = socket.recv()
            socket.send(seed_tuple.encode()) #Send seed so game's randomness is provable
        elif dealer_hand.value < player_hand.value:
            print("player wins")
            socket.send(b'win')
            ignore = socket.recv()
            socket.send(seed_tuple.encode()) #Send seed so game's randomness is provable
        else:
            print("Stalemate")
            socket.send(b'stalemate')
            ignore = socket.recv()
            socket.send(seed_tuple.encode()) #Send seed so game's randomness is provable
        return

if __name__ == "__main__":
    print("ZeroMQ version:", zmq.__version__)
    context = zmq.Context()
    socket = context.socket(zmq.REP)
    socket.bind("tcp://*:12345")
    print("Connected to: tcp://*:12345")
    while True:
        response = socket.recv()
        if response == b'start':
            random.seed()
            main()