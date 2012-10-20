#!/usr/bin/python2.5
#
# Copyright (C) 2009 Roberto Perdisci
# Author: Roberto Perdisci
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

import os
import sys
import gc
import socket
import time
import math
import nmsg
import wdns
import time
import gzip
import threading
import Queue

from threading import Thread
from datetime import datetime, timedelta
from sets import Set

MAX_SUSPICIOUS_TTL = 3*3600
MIN_SUSPICIOUS_RRSET_SIZE = 3
MIN_SUSPICIOUS_DIVERSITY = 1.0/3 
MIN_TOTAL_RRSET_SIZE = 3
MIN_TOTAL_DIVERSITY = 0.5 
MIN_TOTAL_QUERY_VOLUME = 1
VERY_SHORT_TTL = 30
EXPIRATION_WINDOW = 12*3600
EXPIRATION_PROBE = 600

LOG_DIR = 'CANDIDATE_FLUX_DOMAINS'
LOG_FILE_PREFIX = 'SIE_candidate_flux_domains'
LOG_INTERVAL = 3600

domain_queue = Queue.Queue()
candidate_domains = {} 
lock = threading.Lock()



class LogCandidateDomains(Thread):
    
    def __init__(self):
        Thread.__init__(self)
        self.start_time = time.time()
        if not os.path.exists(LOG_DIR):
            os.mkdir(LOG_DIR)

        log_file_name = LOG_FILE_PREFIX+"."+str(int(self.start_time))+".gz"
        self.log_file = gzip.open(os.path.join(LOG_DIR,log_file_name), 'wb')

    def run(self):
        global domain_queue
        global candidate_domains

        while True:
            print "domain_queue size =", domain_queue.qsize()
            print "candidate_domains size =", len(candidate_domains)
            print "-------------------------------------"
            sys.stdout.flush()

            log = domain_queue.get()
            t = time.time() 
        
            if t - self.start_time > LOG_INTERVAL:
                self.log_file.close()
                log_file_name = LOG_FILE_PREFIX+"."+str(int(t))+".gz"
                self.log_file = gzip.open(os.path.join(LOG_DIR,log_file_name), 'wb') 
                self.start_time = t
        
            self.log_file.write(log + '\n')
            self.log_file.flush()



class PruneCandidateDomainList(Thread):

    def __init__(self):
        Thread.__init__(self)


    def run(self):
        global candidate_domains
        global domain_queue
        global lock

        while True:
            print "Checking for prunable candidate domains..."
            sys.stdout.flush()
            

            t = datetime.fromtimestamp(time.time())

            keys = []
            lock.acquire()
            try:
                keys = candidate_domains.keys()
            except:
                pass
            finally:
                lock.release()        

            print "Iterating through the keys; len(keys) =", len(keys)
            sys.stdout.flush()

            for k in keys:
                d = None
                lock.acquire()
                try:
                    d = candidate_domains[k]
                except:
                    pass
                finally:
                    lock.release()

                if d:
                    delta = t - d.first_seen
                    seconds = delta.days * 86400 + delta.seconds
                    if seconds > EXPIRATION_WINDOW:
                        lock.acquire()
                        try:
                            if len(d.rrset) >= MIN_TOTAL_RRSET_SIZE and d.query_volume >= MIN_TOTAL_QUERY_VOLUME and rdata_diversity(d.rrset) >= MIN_TOTAL_DIVERSITY:
                                domain_queue.put(str(d))
                                # print d
                                # sys.stdout.flush()

                            # cleanup the memory
                            del candidate_domains[k]

                            print "Pruned candidate domain", k
                            sys.stdout.flush()


                        except:
                            pass

                        finally:
                            lock.release() 

            print "Activating Garbage Collection..."
            gc.collect()
            print "Done with GC!"
            sys.stdout.flush()

            time.sleep(EXPIRATION_PROBE)




class CandidateDomain:
    
    def __init__(self, qname, qcount, ttl, rrset, first_seen, last_seen, msg_time):
        self.qname = qname
        self.msg_count = 1
        self.min_ttl = ttl
        self.avg_ttl = ttl
        self.max_ttl = ttl
        self.rrset = Set([])
        for rr in rrset:
            rr_str = str(rr)
            self.rrset.add(rr_str)
        self.first_seen = first_seen
        self.last_seen = last_seen
        self.first_msg = msg_time
        self.query_volume = qcount
        self.growth = [len(self.rrset)]

    def update(self, qname, qcount, ttl, rrset, first_seen, last_seen):
        if not self.qname == qname:
            return
    
        self.msg_count += 1

        if self.min_ttl > ttl:
            self.min_ttl = ttl

        if self.max_ttl < ttl:
            self.max_ttl = ttl

        self.avg_ttl = (self.avg_ttl*(self.msg_count-1) + ttl)*1.0/self.msg_count

        for rr in rrset:
            rr_str = str(rr)
            self.rrset.add(rr_str)

        # if len(self.rrset) > self.growth[len(self.growth)-1]:
        self.growth.append(len(self.rrset))

        if self.first_seen > first_seen:
            self.first_seen = first_seen

        if self.last_seen < last_seen:
            self.last_seen = last_seen

        self.query_volume += qcount


    def __str__(self):
        return str(self.qname)+" "+str(self.msg_count)+" "+str(self.query_volume)+" "+str(self.avg_ttl)+" "+str(self.min_ttl)+" "+str(self.max_ttl)+" "+str(self.first_seen)+" "+str(self.last_seen)+" "+str(datetime.fromtimestamp(time.time()))+" "+str(len(self.rrset))+" "+str(self.rrset)+" "+str(self.growth)




def rdata_diversity(rrset):
    if len(rrset) <= 1:
        return 0

    rrlist_16 = []
    for rr in rrset:
        octets = str(rr).split('.')
        if len(octets) != 4:
            return -1

        rrlist_16.append(octets[0] + "." + octets[1])
   
    # compute the entropy of the /16 prefixes
    prob = []
    already_counted = Set([])
    for i in rrlist_16: 
        if not i in already_counted:
            prob.append(rrlist_16.count(i)*1.0/len(rrset))
            already_counted.add(i)
    
    entropy = 0
    for p in prob:
        entropy -= p*math.log(p,2)
    norm_entropy = entropy/math.log(len(rrset),2)

    return norm_entropy



def process_msg(m):

    if m['type'] != "EXPIRATION":
        return 
    
    if m['rrtype'] != 1:
        return

    if m['rrttl'] > MAX_SUSPICIOUS_TTL:
        return

    if len(m['rdata']) < MIN_SUSPICIOUS_RRSET_SIZE and m['rrttl'] > VERY_SHORT_TTL: 
        return

    qname = wdns.domain_to_str(m['rrname'])
    qcount = m['count']
    ttl   = m['rrttl']
    rrset_str = Set([])
    for rr in m['rdata']:
        rr_str = str(wdns.rdata(rr, m['rrclass'], m['rrtype']))
        rrset_str.add(rr_str)

    diversity = rdata_diversity(rrset_str) 
    if diversity < 0:
        return

    if len(m['rdata']) >= MIN_SUSPICIOUS_RRSET_SIZE and diversity < MIN_SUSPICIOUS_DIVERSITY:
        return

    first_seen = datetime.fromtimestamp(m['time_first'])
    last_seen = first_seen
    if 'time_last' in m.keys():
        last_seen  = datetime.fromtimestamp(m['time_last'])
    msg_creation = datetime.fromtimestamp(m.time_sec)
    delta = msg_creation - first_seen
    seconds = delta.days * 86400 + delta.seconds
    if seconds == 0:
        return # we cannot compute the avg num of queries in 1h for this message

    # normalized_1h_count = qcount*1.0/seconds * 3600
    # print qname, ttl, "("+str(normalized_1h_count)+")"
    # print diversity
    # for rr_str in rrset_str:
    #     print rr_str
    # print "-----------------------"

    add_to_candidate_domains(qname, qcount, ttl, rrset_str, first_seen, last_seen, msg_creation)


def add_to_candidate_domains(qname, qcount, ttl, rrset, first_seen, last_seen, msg_time):
    global domain_queue
    global candidate_domains
    global lock
    
    lock.acquire()
    try:
        if not candidate_domains.has_key(qname):
            # print "Initializing", qname
            candidate_domains[qname] = CandidateDomain(qname, qcount, ttl, rrset, first_seen, last_seen, msg_time)

        else:
            d = candidate_domains[qname]
            d.update(qname, qcount, ttl, rrset, first_seen, last_seen)

            if msg_time - d.first_seen >= timedelta(seconds=EXPIRATION_WINDOW) and msg_time - d.first_msg >= timedelta(seconds=EXPIRATION_PROBE):
                # print "Expiring", qname
                if len(d.rrset) >= MIN_TOTAL_RRSET_SIZE and d.query_volume >= MIN_TOTAL_QUERY_VOLUME and rdata_diversity(d.rrset) >= MIN_TOTAL_DIVERSITY:
                    domain_queue.put(str(d))
                    # print d
                    # sys.stdout.flush()

                print "Removing candidate domain", qname
                del candidate_domains[qname]
                # print "Candidate Domains List Size =", len(candidate_domains)

    except:
        pass
    
    finally:
        lock.release()

    


def main():
    domain_collector = PruneCandidateDomainList()
    domain_collector.start()

    domain_logger = LogCandidateDomains()
    domain_logger.start()

    io = nmsg.io()
    # io.add_input_channel('ch208')
    io.add_input_channel('ch204')
    io.set_filter_msgtype('SIE', 'dnsdedupe')
    io.add_output_callback(process_msg)
    io.loop()

if __name__ == '__main__':
    main()
