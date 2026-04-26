package net.arna.jcraft.common.util;

@FunctionalInterface
public interface TriConsumer<A,B,C> {

    void accept(A a, B b, C c);

}
