/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.schedule;

import com.google.common.base.Objects;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.core.ParseField;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.jobscheduler.spi.ScheduledJobParameter;
import org.opensearch.jobscheduler.spi.schedule.CronSchedule;
import org.opensearch.jobscheduler.spi.schedule.IntervalSchedule;
import org.opensearch.jobscheduler.spi.schedule.Schedule;
import org.opensearch.jobscheduler.spi.schedule.ScheduleParser;

import java.io.IOException;
import java.time.Instant;

import static org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken;

/**
 * Sample scheduled job for the HelloWorld Extension
 */
public class GreetJob implements Writeable, ToXContentObject, ScheduledJobParameter {
    enum ScheduleType {
        CRON,
        INTERVAL
    }

    public static final String PARSE_FIELD_NAME = "GreetJob";
    public static final NamedXContentRegistry.Entry XCONTENT_REGISTRY = new NamedXContentRegistry.Entry(
        GreetJob.class,
        new ParseField(PARSE_FIELD_NAME),
        it -> parse(it)
    );

    public static final String HELLO_WORLD_JOB_INDEX = ".hello-world-jobs";
    public static final String NAME_FIELD = "name";
    public static final String LAST_UPDATE_TIME_FIELD = "last_update_time";
    public static final String LOCK_DURATION_SECONDS = "lock_duration_seconds";

    public static final String SCHEDULE_FIELD = "schedule";
    public static final String IS_ENABLED_FIELD = "enabled";
    public static final String ENABLED_TIME_FIELD = "enabled_time";
    public static final String DISABLED_TIME_FIELD = "disabled_time";

    private final String name;
    private final Schedule schedule;
    private final Boolean isEnabled;
    private final Instant enabledTime;
    private final Instant disabledTime;
    private final Instant lastUpdateTime;
    private final Long lockDurationSeconds;

    /**
     *
     * @param name name of the scheduled job
     * @param schedule The schedule, cron or interval, the job run will run with
     * @param isEnabled Flag to indices whether this job is enabled
     * @param enabledTime Timestamp when the job was last enabled
     * @param disabledTime Timestamp when the job was last disabled
     * @param lastUpdateTime Timestamp when the job was last updated
     * @param lockDurationSeconds Time in seconds for how long this job should acquire a lock
     */
    public GreetJob(
        String name,
        Schedule schedule,
        Boolean isEnabled,
        Instant enabledTime,
        Instant disabledTime,
        Instant lastUpdateTime,
        Long lockDurationSeconds
    ) {
        this.name = name;
        this.schedule = schedule;
        this.isEnabled = isEnabled;
        this.enabledTime = enabledTime;
        this.disabledTime = disabledTime;
        this.lastUpdateTime = lastUpdateTime;
        this.lockDurationSeconds = lockDurationSeconds;
    }

    /**
     *
     * @param input The input stream
     * @throws IOException Thrown if there is an error parsing the input stream into a GreetJob
     */
    public GreetJob(StreamInput input) throws IOException {
        name = input.readString();
        if (input.readEnum(ScheduleType.class) == ScheduleType.CRON) {
            schedule = new CronSchedule(input);
        } else {
            schedule = new IntervalSchedule(input);
        }
        isEnabled = input.readBoolean();
        enabledTime = input.readInstant();
        disabledTime = input.readInstant();
        lastUpdateTime = input.readInstant();
        lockDurationSeconds = input.readLong();
    }

    /**
     *
     * @param builder An XContentBuilder instance
     * @param params TOXContent.Params
     * @return
     * @throws IOException
     */
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        XContentBuilder xContentBuilder = builder.startObject()
            .field(NAME_FIELD, name)
            .field(SCHEDULE_FIELD, schedule)
            .field(IS_ENABLED_FIELD, isEnabled)
            .field(ENABLED_TIME_FIELD, enabledTime.toEpochMilli())
            .field(LAST_UPDATE_TIME_FIELD, lastUpdateTime.toEpochMilli())
            .field(LOCK_DURATION_SECONDS, lockDurationSeconds);
        if (disabledTime != null) {
            xContentBuilder.field(DISABLED_TIME_FIELD, disabledTime.toEpochMilli());
        }
        return xContentBuilder.endObject();
    }

    /**
     *
     * @param output The output stream
     * @throws IOException
     */
    @Override
    public void writeTo(StreamOutput output) throws IOException {
        output.writeString(name);
        if (schedule instanceof CronSchedule) {
            output.writeEnum(ScheduleType.CRON);
        } else {
            output.writeEnum(ScheduleType.INTERVAL);
        }
        schedule.writeTo(output);
        output.writeBoolean(isEnabled);
        output.writeInstant(enabledTime);
        output.writeInstant(disabledTime);
        output.writeInstant(lastUpdateTime);
        output.writeLong(lockDurationSeconds);
    }

    /**
     *
     * @param parser Parser that takes builds a GreetJob from XContent
     * @return An instance of a GreetJob
     * @throws IOException
     */
    public static GreetJob parse(XContentParser parser) throws IOException {
        String name = null;
        Schedule schedule = null;
        // we cannot set it to null as isEnabled() would do the unboxing and results in null pointer exception
        Boolean isEnabled = Boolean.FALSE;
        Instant enabledTime = null;
        Instant disabledTime = null;
        Instant lastUpdateTime = null;
        Long lockDurationSeconds = 5L;

        ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.currentToken(), parser);
        while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
            String fieldName = parser.currentName();
            parser.nextToken();

            switch (fieldName) {
                case NAME_FIELD:
                    name = parser.text();
                    break;
                case SCHEDULE_FIELD:
                    schedule = ScheduleParser.parse(parser);
                    break;
                case IS_ENABLED_FIELD:
                    isEnabled = parser.booleanValue();
                    break;
                case ENABLED_TIME_FIELD:
                    enabledTime = toInstant(parser);
                    break;
                case DISABLED_TIME_FIELD:
                    disabledTime = toInstant(parser);
                    break;
                case LAST_UPDATE_TIME_FIELD:
                    lastUpdateTime = toInstant(parser);
                    break;
                case LOCK_DURATION_SECONDS:
                    lockDurationSeconds = parser.longValue();
                    break;
                default:
                    parser.skipChildren();
                    break;
            }
        }
        return new GreetJob(name, schedule, isEnabled, enabledTime, disabledTime, lastUpdateTime, lockDurationSeconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GreetJob that = (GreetJob) o;
        return Objects.equal(getName(), that.getName())
            && Objects.equal(getSchedule(), that.getSchedule())
            && Objects.equal(isEnabled(), that.isEnabled())
            && Objects.equal(getEnabledTime(), that.getEnabledTime())
            && Objects.equal(getDisabledTime(), that.getDisabledTime())
            && Objects.equal(getLastUpdateTime(), that.getLastUpdateTime())
            && Objects.equal(getLockDurationSeconds(), that.getLockDurationSeconds());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, schedule, isEnabled, enabledTime, lastUpdateTime);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Instant getEnabledTime() {
        return enabledTime;
    }

    public Instant getDisabledTime() {
        return disabledTime;
    }

    @Override
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public Long getLockDurationSeconds() {
        return lockDurationSeconds;
    }

    /**
     * Parse content parser to {@link Instant}.
     *
     * @param parser json based content parser
     * @return instance of {@link Instant}
     * @throws IOException IOException if content can't be parsed correctly
     */
    public static Instant toInstant(XContentParser parser) throws IOException {
        if (parser.currentToken() == null || parser.currentToken() == XContentParser.Token.VALUE_NULL) {
            return null;
        }
        if (parser.currentToken().isValue()) {
            return Instant.ofEpochMilli(parser.longValue());
        }
        return null;
    }

    /**
     *
     * @return Returns a plain old java object of a GreetJob for writing to the hello world jobs index
     */
    public GreetJobPojo toPojo() {
        GreetJobPojo.SchedulePojo.IntervalPojo interval = null;
        if (this.schedule instanceof IntervalSchedule) {
            interval = new GreetJobPojo.SchedulePojo.IntervalPojo(
                ((IntervalSchedule) this.schedule).getUnit().toString(),
                ((IntervalSchedule) this.schedule).getInterval(),
                ((IntervalSchedule) this.schedule).getStartTime().toEpochMilli()
            );
        }
        return new GreetJobPojo(
            this.enabledTime.toEpochMilli(),
            this.lastUpdateTime.toEpochMilli(),
            this.name,
            this.lockDurationSeconds.intValue(),
            this.isEnabled.booleanValue(),
            new GreetJobPojo.SchedulePojo(interval)
        );
    }

    /**
     * A plain java representation of a GreetJob using only primitives
     */
    public static class GreetJobPojo {
        public long enabled_time;
        public long last_update_time;

        public String name;

        public int lock_duration_seconds;

        public boolean enabled;

        public SchedulePojo schedule;

        /**
         *
         * @param enabledTime
         * @param lastUpdateTime
         * @param name
         * @param lockDurationSeconds
         * @param enabled
         * @param schedule
         */
        public GreetJobPojo(
            long enabledTime,
            long lastUpdateTime,
            String name,
            int lockDurationSeconds,
            boolean enabled,
            SchedulePojo schedule
        ) {
            this.enabled_time = enabledTime;
            this.last_update_time = lastUpdateTime;
            this.name = name;
            this.lock_duration_seconds = lockDurationSeconds;
            this.enabled = enabled;
            this.schedule = schedule;
        }

        /**
         * A plain java representation of a Schedule using only primitives
         */
        public static class SchedulePojo {

            public IntervalPojo interval;

            /**
             *
             * @param interval An Interval instance
             */
            public SchedulePojo(IntervalPojo interval) {
                this.interval = interval;
            }

            /**
             * A plain java representation of a Interval using only primitives
             */
            public static class IntervalPojo {
                public String unit;

                public int period;

                public long start_time;

                /**
                 *
                 * @param unit Unit of time
                 * @param period Number of units between job execution
                 * @param start_time The time when the interval first started
                 */
                public IntervalPojo(String unit, int period, long start_time) {
                    this.unit = unit;
                    this.period = period;
                    this.start_time = start_time;
                }
            }
        }
    }
}
